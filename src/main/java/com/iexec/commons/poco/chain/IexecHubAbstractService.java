/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.commons.poco.chain;

import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.MultiAddressHelper;
import com.iexec.commons.poco.utils.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.crypto.Credentials;
import org.web3j.ens.EnsResolutionException;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iexec.commons.poco.encoding.AccessorsEncoder.*;
import static com.iexec.commons.poco.tee.TeeEnclaveConfiguration.buildEnclaveConfigurationFromJsonString;
import static com.iexec.commons.poco.utils.BytesUtils.isNonZeroedBytes32;

/*
 * Contracts (located at *.contract.generated) which are used in this service are generated from:
 * - https://github.com/iExecBlockchainComputing/PoCo-dev
 * - @ commit c989a8d03410c0cc6c67f7b6a56ef891fc3f964c (HEAD, tag: v5.1.0, origin/v5, origin/HEAD, v5)
 * */
@Slf4j
public abstract class IexecHubAbstractService {

    public static final int POLLING_ATTEMPTS_PER_TX_HASH = 12;
    public static final int NB_BLOCKS_TO_WAIT_PER_RETRY = 6;
    public static final int MAX_RETRIES = 3;

    protected final Credentials credentials;
    private final String iexecHubAddress;
    protected final RawTransactionManager txManager;
    protected final PollingTransactionReceiptProcessor txReceiptProcessor;
    protected final IexecHubContract iexecHubContract;
    private final Web3jAbstractService web3jAbstractService;
    private long maxNbOfPeriodsForConsensus = -1;
    private final long retryDelay;// ms
    private final int maxRetries;
    private final Map<Long, ChainCategory> categories = new HashMap<>();
    private final Map<String, TaskDescription> taskDescriptions = new HashMap<>();

    protected IexecHubAbstractService(
            Credentials credentials,
            Web3jAbstractService web3jAbstractService,
            String iexecHubAddress) {
        this(credentials, web3jAbstractService, iexecHubAddress, NB_BLOCKS_TO_WAIT_PER_RETRY, MAX_RETRIES);
    }

    /**
     * Base constructor for the IexecHubAbstractService
     *
     * @param credentials            credentials for sending transaction
     * @param web3jAbstractService   custom web3j service
     * @param iexecHubAddress        address of the iExec Hub contract
     * @param nbBlocksToWaitPerRetry nb block to wait per retry
     * @param maxRetries             maximum reties
     */
    protected IexecHubAbstractService(
            Credentials credentials,
            Web3jAbstractService web3jAbstractService,
            String iexecHubAddress,
            int nbBlocksToWaitPerRetry,
            int maxRetries) {
        this.credentials = credentials;
        this.web3jAbstractService = web3jAbstractService;
        this.iexecHubAddress = iexecHubAddress;
        this.retryDelay = nbBlocksToWaitPerRetry * this.web3jAbstractService.getBlockTime().toMillis();
        this.maxRetries = maxRetries;

        txReceiptProcessor = new PollingTransactionReceiptProcessor(
                web3jAbstractService.getWeb3j(),
                web3jAbstractService.getBlockTime().toMillis(),
                POLLING_ATTEMPTS_PER_TX_HASH
        );

        txManager = new RawTransactionManager(
                web3jAbstractService.getWeb3j(),
                credentials,
                web3jAbstractService.getChainId(),
                txReceiptProcessor
        );

        iexecHubContract = getHubContract(web3jAbstractService.getContractGasProvider());

        log.info("Abstract IexecHubService initialized (iexec proxy address) [hubAddress:{}]",
                iexecHubContract.getContractAddress());
    }

    private static int scoreToWeight(int workerScore) {
        return Math.max(workerScore / 3, 3) - 1;
    }

    /**
     * Get an IexecHubContract instance.
     *
     * @param contractGasProvider gas provider, useful for sending txs
     * @return an IexecHubContract instance
     */
    private IexecHubContract getHubContract(ContractGasProvider contractGasProvider) {
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load IexecHub " +
                        "contract from address " + iexecHubAddress);

        if (iexecHubAddress != null && !iexecHubAddress.isEmpty()) {
            try {
                return IexecHubContract.load(iexecHubAddress,
                        web3jAbstractService.getWeb3j(),
                        txManager,
                        contractGasProvider);
            } catch (EnsResolutionException e) {
                log.warn("EnsResolution error", e);
                throw exceptionInInitializerError;
            }
        } else {
            throw exceptionInInitializerError;
        }
    }

    /*
     * This method should only be used for reading
     */
    public IexecHubContract getHubContract() {
        return iexecHubContract;
    }

    /**
     * Retrieves on-chain deal with a retryer
     *
     * @param chainDealId deal ID
     * @param retryDelay  delay between retries in ms
     * @param maxRetry    number of maximum retry
     * @return optional ChainDeal
     */
    Optional<ChainDeal> repeatGetChainDeal(String chainDealId,
                                           long retryDelay,
                                           int maxRetry) {
        return new Retryer<Optional<ChainDeal>>()
                .repeatCall(() -> getChainDealWithDetails(chainDealId),
                        Optional::isEmpty,
                        retryDelay, maxRetry,
                        String.format("getChainDeal(chainDealId) [chainDealId:%s]", chainDealId));
    }

    /**
     * Retrieves on-chain deal from its blockchain ID
     * <p>
     * The obtained deal won't contain app or dataset details
     * <p>
     * Note:
     * If `start time` is invalid, it is likely a blockchain issue.
     * In this case, in order to protect workflows based on top of it,
     * an {@code Optional.empty()} will be returned.
     *
     * @param chainDealId blockchain ID of the deal (e.g: 0x123..abc)
     * @return deal object
     */
    public Optional<ChainDeal> getChainDeal(String chainDealId) {
        final byte[] chainDealIdBytes = BytesUtils.stringToBytes(chainDealId);
        try {
            final IexecHubContract.Deal deal = iexecHubContract.viewDeal(chainDealIdBytes).send();
            final ChainCategory category = getChainCategory(deal.category.longValue()).orElse(null);
            final ChainDeal chainDeal = ChainDeal.parts2ChainDeal(chainDealId, deal, category);
            return validateChainDeal(chainDeal);
        } catch (Exception e) {
            log.error("Failed to getChainDeal [chainDealId:{}]", chainDealId, e);
        }
        return Optional.empty();
    }

    /**
     * Retrieves on-chain deal from its blockchain ID
     * <p>
     * The obtained deal will contain app or dataset details
     * <p>
     * Note:
     * If `start time` is invalid, it is likely a blockchain issue.
     * In this case, in order to protect workflows based on top of it,
     * an {@code Optional.empty()} will be returned.
     *
     * @param chainDealId blockchain ID of the deal (e.g: 0x123..abc)
     * @return deal object
     */
    public Optional<ChainDeal> getChainDealWithDetails(String chainDealId) {
        final byte[] chainDealIdBytes = BytesUtils.stringToBytes(chainDealId);
        try {
            final IexecHubContract.Deal deal = iexecHubContract.viewDeal(chainDealIdBytes).send();

            final ChainApp app = getChainApp(deal.app.pointer).orElse(null);
            if (app == null) {
                return Optional.empty();
            }
            final ChainCategory category = getChainCategory(deal.category.longValue()).orElse(null);
            if (category == null) {
                return Optional.empty();
            }
            final ChainDataset dataset = getChainDataset(deal.dataset.pointer).orElse(null);

            final ChainDeal chainDeal = ChainDeal.parts2ChainDeal(chainDealId, deal, app, category, dataset);

            return validateChainDeal(chainDeal);
        } catch (Exception e) {
            log.error("Failed to get ChainDeal [chainDealId:{}]", chainDealId, e);
        }
        return Optional.empty();
    }

    /**
     * Checks if deal is valid, i.e. has a positive start time allowing to compute deadlines.
     *
     * @param chainDeal The {@code ChainDeal} to check
     * @return {@code Optional.of(chainDeal)} if valid, {@code Optional.empty()} otherwise
     */
    private Optional<ChainDeal> validateChainDeal(final ChainDeal chainDeal) {
        if (chainDeal.getStartTime() == null || chainDeal.getStartTime().longValue() <= 0) {
            log.error("Deal start time should be greater than zero (likely a blockchain issue) [chainDealId:{}, startTime:{}]",
                    chainDeal.getChainDealId(), chainDeal.getStartTime());
            return Optional.empty();
        }
        return Optional.of(chainDeal);
    }

    /**
     * Retrieve on-chain task with a retryer
     *
     * @param chainTaskId task ID
     * @param retryDelay  delay between retries in ms
     * @param maxRetry    number of maximum retry
     * @return optional ChainTask
     */
    Optional<ChainTask> repeatGetChainTask(String chainTaskId,
                                           long retryDelay,
                                           int maxRetry) {
        return new Retryer<Optional<ChainTask>>()
                .repeatCall(() -> getChainTask(chainTaskId),
                        Optional::isEmpty,
                        retryDelay, maxRetry,
                        String.format("getChainTask(chainTaskId) [chainTaskId:%s]", chainTaskId));
    }

    public Optional<ChainTask> getChainTask(String chainTaskId) {
        try {
            final ChainTask chainTask = ChainTask.tuple2ChainTask(iexecHubContract
                    .viewTaskABILegacy(BytesUtils.stringToBytes(chainTaskId)).send());
            final String chainDealId = chainTask.getDealid();
            if (isNonZeroedBytes32(chainDealId)) {
                return Optional.of(chainTask);
            } else {
                log.debug("Failed to get consistent ChainTask [chainTaskId:{}]",
                        chainTaskId);
            }
        } catch (Exception e) {
            log.error("Failed to get ChainTask [chainTaskId:{}]", chainTaskId, e);
        }
        return Optional.empty();
    }

    public Optional<ChainAccount> getChainAccount(String walletAddress) {
        try {
            return Optional.of(ChainAccount.tuple2Account(
                    iexecHubContract.viewAccountABILegacy(walletAddress).send()));
        } catch (Exception e) {
            log.error("Failed to get ChainAccount [walletAddress:{}]", walletAddress, e);
        }
        return Optional.empty();
    }

    public Optional<ChainContribution> getChainContribution(String chainTaskId,
                                                            String workerAddress) {
        try {
            return Optional.of(ChainContribution.tuple2Contribution(
                    iexecHubContract.viewContributionABILegacy(
                            BytesUtils.stringToBytes(chainTaskId), workerAddress).send()));
        } catch (Exception e) {
            log.error("Failed to get ChainContribution [chainTaskId:{}" +
                    ", workerAddress:{}]", chainTaskId, workerAddress, e);
        }
        return Optional.empty();
    }

    /**
     * Retrieves on-chain category with its blockchain ID from cache
     * <p>
     * If no key exists for the category, the {@link #retrieveCategory(long)} method is called
     * to fetch the category properties in the PoCo smart contracts.
     *
     * @param id blockchain ID of the category
     * @return category object
     */
    public Optional<ChainCategory> getChainCategory(final long id) {
        if (!categories.containsKey(id)) {
            retrieveCategory(id);
        }
        return Optional.ofNullable(categories.get(id));
    }

    /**
     * Retrieves on-chain category with its blockchainID and add it to cache
     * <p>
     * Note:
     * If `max execution time` is invalid, it is likely a blockchain issue.
     * In this case, in order to protect workflows based on top of it, the cache won't be updated
     * to allow another try on next request.
     *
     * @param id blockchain ID of the category
     */
    private void retrieveCategory(final long id) {
        try {
            Tuple3<String, String, BigInteger> category = iexecHubContract
                    .viewCategoryABILegacy(BigInteger.valueOf(id)).send();
            ChainCategory chainCategory = ChainCategory.tuple2ChainCategory(id,
                    category.component1(),
                    category.component2(),
                    category.component3()
            );
            if (chainCategory.getMaxExecutionTime() <= 0) {
                log.error("Category max execution time should be greater than zero " +
                                "(likely a blockchain issue) [categoryId:{}, maxExecutionTime:{}]",
                        id, chainCategory.getMaxExecutionTime());
            }
            categories.put(id, chainCategory);
        } catch (Exception e) {
            log.error("Failed to get all categories", e);
        }
    }

    public Optional<ChainApp> getChainApp(final String appAddress) {
        if (appAddress == null || appAddress.equals(BytesUtils.EMPTY_ADDRESS)) {
            return Optional.empty();
        }
        final ChainApp.ChainAppBuilder chainAppBuilder = ChainApp.builder();
        try {
            chainAppBuilder
                    .chainAppId(appAddress)
                    .type(sendCallAndDecodeDynamicBytes(appAddress, M_APPTYPE_SELECTOR))
                    .multiaddr(sendCallAndDecodeDynamicBytes(appAddress, M_APPMULTIADDR_SELECTOR))
                    .checksum(sendCallAndGetRawResult(appAddress, M_APPCHECKSUM_SELECTOR));
        } catch (Exception e) {
            log.error("Failed to get chain app [chainAppId:{}]",
                    appAddress, e);
            return Optional.empty();
        }
        String mrEnclave;
        try {
            mrEnclave = sendCallAndDecodeDynamicBytes(appAddress, M_APPMRENCLAVE_SELECTOR);
        } catch (Exception e) {
            log.error("Failed to get chain app mrenclave [chainAppId:{}]",
                    appAddress, e);
            return Optional.empty();
        }
        if (StringUtils.isEmpty(mrEnclave)) {
            // Standard application
            return Optional.of(chainAppBuilder.build());
        }
        try {
            chainAppBuilder.enclaveConfiguration(
                    buildEnclaveConfigurationFromJsonString(mrEnclave));
        } catch (Exception e) {
            log.error("Failed to get tee chain app enclave configuration [chainAppId:{}, mrEnclave:{}]",
                    appAddress, mrEnclave, e);
            return Optional.empty();
        }
        return Optional.of(chainAppBuilder.build());
    }

    public Optional<ChainDataset> getChainDataset(final String datasetAddress) {
        if (datasetAddress != null && !datasetAddress.equals(BytesUtils.EMPTY_ADDRESS)) {
            try {
                return Optional.of(ChainDataset.builder()
                        .chainDatasetId(datasetAddress)
                        .multiaddr(sendCallAndDecodeDynamicBytes(datasetAddress, M_DATASETMULTIADDR_SELECTOR))
                        .checksum(sendCallAndGetRawResult(datasetAddress, M_DATASETCHECKSUM_SELECTOR))
                        .build());
            } catch (Exception e) {
                log.error("Failed to get ChainDataset [chainDatasetId:{}]",
                        datasetAddress, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Send a call to a Smart contract to retrieve a single value corresponding to a dynamic type and decode it.
     *
     * @param address  Smart Contract address (can be an App or a Dataset in PoCo)
     * @param selector Function selector
     * @return The decoded String result returned by the call
     * @throws IOException on communication error
     */
    private String sendCallAndDecodeDynamicBytes(final String address, final String selector) throws IOException {
        return MultiAddressHelper.convertToURI(
                FunctionReturnDecoder.decodeDynamicBytes(sendCallAndGetRawResult(address, selector)));
    }

    /**
     * Send a call to a Smart contract to retrieve a single value.
     *
     * @param address  Smart Contract address (can be an App or a Dataset in PoCo)
     * @param selector Function selector
     * @return The hexadecimal representation of retrieved bytes, may need further decoding
     * @throws IOException on communication error
     */
    private String sendCallAndGetRawResult(final String address, final String selector) throws IOException {
        return txManager.sendCall(address, selector, DefaultBlockParameterName.LATEST);
    }

    public Optional<Integer> getWorkerScore(String address) {
        if (address != null && !address.isEmpty()) {
            try {
                BigInteger workerScore = iexecHubContract.viewScore(address).send();
                return Optional.of(workerScore.intValue());
            } catch (Exception e) {
                log.error("Failed to getWorkerScore [address:{}]", address, e);
            }
        }
        return Optional.empty();
    }

    public int getWorkerWeight(String address) {
        Optional<Integer> workerScore = getWorkerScore(address);
        if (workerScore.isEmpty()) {
            return 0;
        }
        int weight = scoreToWeight(workerScore.get());
        log.info("Get worker weight [address:{}, score:{}, weight:{}]",
                address, workerScore.get(), weight);
        return weight;
    }

    /**
     * get the value of MaxNbOfPeriodsForConsensus
     * written onchain.
     *
     * @return the value found onchain or -1 if
     * we could not read it.
     */
    public long getMaxNbOfPeriodsForConsensus() {
        if (maxNbOfPeriodsForConsensus == -1) {
            setMaxNbOfPeriodsForConsensus();
        }
        return maxNbOfPeriodsForConsensus;
    }

    private void setMaxNbOfPeriodsForConsensus() {
        try {
            maxNbOfPeriodsForConsensus = getContributionDeadlineRatio().longValue();
        } catch (Exception e) {
            log.error("Failed to get maxNbOfPeriodsForConsensus from the chain", e);
            maxNbOfPeriodsForConsensus = -1;
        }
    }

    public boolean hasEnoughGas(String address) {
        return web3jAbstractService.hasEnoughGas(address);
    }

    /**
     * Behaves as a cache to avoid always calling blockchain to retrieve task description
     */
    public TaskDescription getTaskDescription(String chainTaskId) {
        if (!taskDescriptions.containsKey(chainTaskId)) {
            repeatGetTaskDescriptionFromChain(chainTaskId, retryDelay, maxRetries)
                    .ifPresent(taskDescription ->
                            taskDescriptions.putIfAbsent(chainTaskId, taskDescription));
        }
        return taskDescriptions.get(chainTaskId);
    }

    Optional<TaskDescription> repeatGetTaskDescriptionFromChain(String chainTaskId,
                                                                long retryDelay,
                                                                int maxRetry) {
        // If retryDelay is 0, a runtime exception will be thrown from failsafe library
        if (retryDelay == 0) {
            log.warn("retry delay cannot be 0 [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }
        final ChainTask chainTask = repeatGetChainTask(chainTaskId, retryDelay, maxRetry).orElse(null);
        if (chainTask == null) {
            log.info("Failed to get TaskDescription, ChainTask error [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }

        final ChainDeal chainDeal = repeatGetChainDeal(chainTask.getDealid(), retryDelay, maxRetry).orElse(null);
        if (chainDeal == null) {
            log.info("Failed to get TaskDescription, ChainDeal error [chainTaskId:{}]", chainTaskId);
            return Optional.empty();
        }

        final TaskDescription taskDescription = TaskDescription.toTaskDescription(chainDeal, chainTask);
        // taskDescription cannot be null here as chainTask and ChainDeal are not
        return taskDescription != null ? Optional.of(taskDescription) : Optional.empty();
    }

    public boolean isTeeTask(String chainTaskId) {
        final TaskDescription taskDescription = getTaskDescription(chainTaskId);

        if (taskDescription == null) {
            log.error("Couldn't get task description from chain [chainTaskId:{}]",
                    chainTaskId);
            return false;
        }

        return taskDescription.isTeeTask();
    }

    // region accessors

    /**
     * Send call to callbackgas() PoCo method.
     *
     * @return callbackgas value
     * @throws IOException if communication fails
     */
    public BigInteger getCallbackGas() throws IOException {
        return sendCallWithFunctionSelector(CALLBACKGAS_SELECTOR);
    }

    /**
     * Send call to contribution_deadline_ratio() PoCo method.
     *
     * @return contribution_deadline_ratio value
     * @throws IOException if communication fails
     */
    public BigInteger getContributionDeadlineRatio() throws IOException {
        return sendCallWithFunctionSelector(CONTRIBUTION_DEADLINE_RATIO_SELECTOR);
    }

    /**
     * Send call to final_deadline_ratio() PoCo method.
     *
     * @return final_deadline_ratio value
     * @throws IOException if communication fails
     */
    public BigInteger getFinalDeadlineRatio() throws IOException {
        return sendCallWithFunctionSelector(FINAL_DEADLINE_RATIO_SELECTOR);
    }

    private BigInteger sendCallWithFunctionSelector(final String functionSelector) throws IOException {
        return Numeric.toBigInt(
                txManager.sendCall(iexecHubAddress, functionSelector, DefaultBlockParameterName.LATEST));
    }

    public String getOwner(final String address) {
        try {
            return Numeric.toHexStringWithPrefixZeroPadded(
                    Numeric.toBigInt(txManager.sendCall(address, OWNER_SELECTOR, DefaultBlockParameterName.LATEST)), 40);
        } catch (Exception e) {
            log.error("Failed to get owner [address:{}]", address, e);
        }
        return "";
    }

    // endregion

    // region Purge

    /**
     * Purge description of given task.
     *
     * @param chainTaskId ID of the task to purge.
     * @return {@literal true} if task description was cached
     * and has been purged;
     * {@literal false} otherwise.
     */
    protected boolean purgeTask(String chainTaskId) {
        if (!taskDescriptions.containsKey(chainTaskId)) {
            log.info("Can't purge task description [chainTaskId:{}]", chainTaskId);
            return false;
        }

        return taskDescriptions.remove(chainTaskId) != null;
    }

    /**
     * Purge all cached task descriptions.
     */
    protected void purgeAllTasksData() {
        taskDescriptions.clear();
    }

    // endregion
}
