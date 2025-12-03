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
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.encoding.MatchOrdersDataEncoder;
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.Retryer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.ens.EnsResolutionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.iexec.commons.poco.chain.Web3jAbstractService.toBigInt;
import static com.iexec.commons.poco.chain.Web3jAbstractService.toEthereumAddress;
import static com.iexec.commons.poco.encoding.AccessorsEncoder.*;
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
    protected final RawTransactionManager txManager;
    protected final PollingTransactionReceiptProcessor txReceiptProcessor;
    protected final IexecHubContract iexecHubContract;
    protected final String iexecHubAddress;
    @Getter
    private final EIP712Domain ordersDomain;
    private final Web3jAbstractService web3jAbstractService;
    private long maxNbOfPeriodsForConsensus = -1;
    private final long retryDelay;// ms
    private final int maxRetries;
    private final Map<Long, ChainCategory> categories = new ConcurrentHashMap<>();
    private final Map<String, TaskDescription> taskDescriptions = new ConcurrentHashMap<>();

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
        ordersDomain = new EIP712Domain(web3jAbstractService.getChainId(), iexecHubAddress);

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
    Optional<ChainDeal> repeatGetChainDeal(final String chainDealId,
                                           final long retryDelay,
                                           final int maxRetry) {
        return new Retryer<Optional<ChainDeal>>()
                .repeatCall(() -> getChainDeal(chainDealId),
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
     * @deprecated on-chain app, category, dataset and deal are fetched separately (see repeatGetTaskDescriptionFromChain)
     */
    @Deprecated(forRemoval = true)
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
    Optional<ChainTask> repeatGetChainTask(final String chainTaskId,
                                           final long retryDelay,
                                           final int maxRetry) {
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

    public Optional<ChainAccount> getChainAccount(final String walletAddress) {
        try {
            final String txData = VIEW_ACCOUNT_SELECTOR +
                    Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(walletAddress), 64);
            return Optional.ofNullable(ChainAccount.fromRawData(
                    web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData)));
        } catch (Exception e) {
            log.error("Failed to get ChainAccount [walletAddress:{}]", walletAddress, e);
        }
        return Optional.empty();
    }

    public Optional<ChainContribution> getChainContribution(final String chainTaskId,
                                                            final String workerAddress) {
        try {
            final String txData = VIEW_CONTRIBUTION_SELECTOR +
                    Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(chainTaskId), 64) +
                    Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(workerAddress), 64);
            final String rawData = web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData);
            return Optional.ofNullable(ChainContribution.fromRawData(rawData));
        } catch (Exception e) {
            log.error("Failed to get ChainContribution [chainTaskId:{}, workerAddress:{}]",
                    chainTaskId, workerAddress, e);
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
            final String txData = VIEW_CATEGORY_SELECTOR + Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(id), 64);
            final ChainCategory chainCategory = ChainCategory.fromRawData(
                    id, web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData));
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
        try {
            final String txData = VIEW_APP_SELECTOR +
                    Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(appAddress), 64);
            final String rawData = web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData);
            return Optional.of(ChainApp.fromRawData(appAddress, rawData));
        } catch (Exception e) {
            log.error("Failed to get chain app [chainAppId:{}]",
                    appAddress, e);
            return Optional.empty();
        }
    }

    public Optional<ChainDataset> getChainDataset(final String datasetAddress) {
        if (datasetAddress != null && !datasetAddress.equals(BytesUtils.EMPTY_ADDRESS)) {
            try {
                final String txData = VIEW_DATASET_SELECTOR +
                        Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(datasetAddress), 64);
                final String rawData = web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData);
                return Optional.of(ChainDataset.fromRawData(datasetAddress, rawData));
            } catch (Exception e) {
                log.error("Failed to get ChainDataset [chainDatasetId:{}]",
                        datasetAddress, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Read worker score.
     * <p>
     * The score only changes in replicated deals when an actual replication occurs.
     *
     * @param address Worker address
     * @return The worker score
     * @see <a href="https://github.com/iExecBlockchainComputing/PoCo/blob/v6.1.0-contracts/contracts/facets/IexecPoco2Facet.sol#L462">distributeRewards</a>
     */
    public Optional<Integer> getWorkerScore(final String address) {
        if (address != null && !address.isEmpty()) {
            try {
                final String txData = VIEW_SCORE_SELECTOR +
                        Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(address), 64);
                final BigInteger workerScore = toBigInt(web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData));
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

    /**
     * Retrieves task, deal, category, app and dataset models on PoCo Smart Contracts to build a task description.
     *
     * @param chainTaskId ID of the task
     * @param retryDelay  Interval between consecutive attempts while reading on the blockchain network
     * @param maxRetry    Maximum number of attempts
     * @return The aggregate {@code TaskDescription}.
     * If the maximum number of attempts is reached without retrieving data, an empty result will be returned.
     */
    Optional<TaskDescription> repeatGetTaskDescriptionFromChain(final String chainTaskId,
                                                                final long retryDelay,
                                                                final int maxRetry) {
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

        final ChainCategory chainCategory = new Retryer<Optional<ChainCategory>>()
                .repeatCall(() -> getChainCategory(chainDeal.getCategory().longValue()),
                        Optional::isEmpty,
                        retryDelay, maxRetry,
                        String.format("getChainCategory() [category:%s]", chainDeal.getCategory().longValue()))
                .orElse(null);

        final ChainApp chainApp = new Retryer<Optional<ChainApp>>()
                .repeatCall(() -> getChainApp(chainDeal.getDappPointer()),
                        Optional::isEmpty,
                        retryDelay, maxRetry,
                        String.format("getChainApp() [address:%s]", chainDeal.getDappPointer()))
                .orElse(null);

        final ChainDataset chainDataset = !chainDeal.containsDataset() ? null : new Retryer<Optional<ChainDataset>>()
                .repeatCall(() -> getChainDataset(chainDeal.getDataPointer()),
                        Optional::isEmpty,
                        retryDelay, maxRetry,
                        String.format("getChainDataset() [address:%s]", chainDeal.getDataPointer()))
                .orElse(null);

        final TaskDescription taskDescription = TaskDescription.toTaskDescription(
                chainDeal, chainTask, chainCategory, chainApp, chainDataset);
        // taskDescription cannot be null here as chainTask and ChainDeal are not
        return Optional.ofNullable(taskDescription);
    }

    /**
     * @deprecated single usage found in all code, directly call isTeeTask on getTaskDescription
     */
    @Deprecated(forRemoval = true)
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

    public void assertDatasetDealCompatibility(final DatasetOrder datasetOrder, final String dealId) throws IOException {
        final String txData = MatchOrdersDataEncoder.encodeAssertDatasetDealCompatibility(datasetOrder, dealId);
        web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, txData);
    }

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
        return toBigInt(web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, functionSelector));
    }

    public String getOwner(final String address) {
        try {
            return toEthereumAddress(web3jAbstractService.sendCall(credentials.getAddress(), address, OWNER_SELECTOR));
        } catch (Exception e) {
            log.error("Failed to get owner [address:{}]", address, e);
        }
        return "";
    }

    /**
     * Read on-chain the consumption level of an order from its EIP-712 hash
     *
     * @param typedHash The order EIP-712 hash whose consumption level is queried
     * @return The consumed value which is less or equal to the order volume. It will be {@literal BigInteger.ZERO} if the
     * hash is not present on-chain and has never been matched in a deal.
     * @throws IOException on communication error with the blockchain network
     */
    public BigInteger viewConsumed(final String typedHash) throws IOException {
        final String payload = VIEW_CONSUMED_SELECTOR + Numeric.cleanHexPrefix(typedHash);
        return toBigInt(web3jAbstractService.sendCall(credentials.getAddress(), iexecHubAddress, payload));
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
