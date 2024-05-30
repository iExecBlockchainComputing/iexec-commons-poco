/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.contract.IexecHubSmartContractValidator;
import com.iexec.commons.poco.contract.generated.*;
import com.iexec.commons.poco.task.TaskDescription;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.web3j.crypto.Credentials;
import org.web3j.ens.EnsResolutionException;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.encoding.AssetDataEncoder.getAssetAddressFromReceipt;
import static com.iexec.commons.poco.tee.TeeEnclaveConfiguration.buildEnclaveConfigurationFromJsonString;
import static com.iexec.commons.poco.utils.BytesUtils.isNonZeroedBytes32;
import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;


/*
 * Contracts (located at *.contract.generated) which are used in this service are generated from:
 * - https://github.com/iExecBlockchainComputing/PoCo-dev
 * - @ commit c989a8d03410c0cc6c67f7b6a56ef891fc3f964c (HEAD, tag: v5.1.0, origin/v5, origin/HEAD, v5)
 * */
@Slf4j
public abstract class IexecHubAbstractService {

    public static final int NB_BLOCKS_TO_WAIT_PER_RETRY = 6;
    public static final int MAX_RETRIES = 3;

    protected final Credentials credentials;
    private final String iexecHubAddress;
    private final RawTransactionManager txManager;
    protected IexecHubContract iexecHubContract;
    private final Web3jAbstractService web3jAbstractService;
    private long maxNbOfPeriodsForConsensus = -1;
    private final long retryDelay;// ms
    private final int maxRetries;
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

        txManager = new RawTransactionManager(
                web3jAbstractService.getWeb3j(),
                credentials,
                web3jAbstractService.getChainId(),
                DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH,
                web3jAbstractService.getBlockTime().toMillis()
        );

        iexecHubContract = getHubContract(web3jAbstractService.getContractGasProvider());

        log.info("Abstract IexecHubService initialized (iexec proxy address) [hubAddress:{}]",
                iexecHubContract.getContractAddress());
    }

    @PostConstruct
    private void validateRemoteIexecHubSmartContract() {
        if (!new IexecHubSmartContractValidator().validate(iexecHubContract)) {
            throw new IllegalArgumentException(
                    "IexecHub smart contract validation failed."
            );
        }
        setMaxNbOfPeriodsForConsensus();
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

    // region workerpool

    // TODO move workerpool methods to their own class (e.g.: WorkerpoolManager)
    public Workerpool getWorkerpoolContract(String workerpoolAddress) {
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load Workerpool " +
                        "contract address " + workerpoolAddress);
        try {
            if (workerpoolAddress == null || workerpoolAddress.isEmpty()) {
                throw exceptionInInitializerError;
            }

            return Workerpool.load(workerpoolAddress,
                    web3jAbstractService.getWeb3j(),
                    credentials,
                    new DefaultGasProvider());
        } catch (Exception e) {
            log.error("Failed to load chainWorkerpool [address:{}]", workerpoolAddress, e);
        }
        return null;
    }

    public WorkerpoolRegistry getWorkerpoolRegistryContract(ContractGasProvider contractGasProvider) {
        String workerpoolRegistryAddress = "";
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load WorkerpoolRegistry contract");
        try {
            workerpoolRegistryAddress = iexecHubContract.workerpoolregistry().send();
            if (workerpoolRegistryAddress == null || workerpoolRegistryAddress.isEmpty()) {
                throw exceptionInInitializerError;
            }
            return WorkerpoolRegistry.load(
                    workerpoolRegistryAddress,
                    web3jAbstractService.getWeb3j(),
                    txManager,
                    contractGasProvider);
        } catch (Exception e) {
            log.error("Failed to load WorkerpoolRegistry contract [address:{}]",
                    workerpoolRegistryAddress, e);
        }
        return null;
    }

    /**
     * This method allows to create a new workerpool on iExec
     * <p>
     * Note: Workerpool is an ERC721. We use the Transfer event sent in the
     * ERC721 mint method to retrieve workerpool address
     * tokenId is the generic form of workerpoolAddress
     *
     * @param name                   workerpool name
     * @param secondsTimeout         await workerpool deployment for couple seconds
     * @param secondsPollingInterval check if workerpool is deployed every couple seconds
     * @return workerpool address (e.g.: 0x95ba540ca3c2dfd52a7e487a03e1358dfe9441ce)
     */
    public String createWorkerpool(String name, int secondsTimeout, int secondsPollingInterval) {
        String owner = credentials.getAddress();
        final String paramsPrinter = " [owner:{}, name:{}]";

        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(name)) {
            log.error("Non empty inputs are required" + paramsPrinter, owner, name);
            return "";
        }

        WorkerpoolRegistry workerpoolRegistry =
                getWorkerpoolRegistryContract(web3jAbstractService.getContractGasProvider());
        if (workerpoolRegistry == null) {
            log.error("Failed to get workerpoolRegistry" + paramsPrinter, owner, name);
            return "";
        }

        RemoteCall<TransactionReceipt> createWorkerpoolCall = workerpoolRegistry
                .createWorkerpool(owner, name);

        TransactionReceipt createWorkerpoolReceipt;
        try {
            createWorkerpoolReceipt = createWorkerpoolCall.send();
        } catch (Exception e) {
            log.error("Failed to send createWorkerpool transaction" + paramsPrinter,
                    owner, name, e);
            return "";
        }

        if (!createWorkerpoolReceipt.isStatusOK()) {
            log.error("Bad response status for createWorkerpool transaction" + paramsPrinter,
                    owner, name);
            return "";
        }

        String workerpoolAddress = getAssetAddressFromReceipt(createWorkerpoolReceipt);

        if (StringUtils.isEmpty(workerpoolAddress)) {
            log.error("Failed to extract workerpool address" + paramsPrinter,
                    owner, name);
            return "";
        }

        //tx hash can be null, manually verifying contract is deployed
        Callable<Optional<ChainWorkerpool>> isDeployedWorkerpool = () -> {
            log.info("Waiting for contract deployment" + paramsPrinter,
                    owner, name);
            return getChainWorkerpool(getWorkerpoolContract(workerpoolAddress));
        };

        try {
            Awaitility.await()
                    .atMost(secondsTimeout, TimeUnit.SECONDS)
                    .pollInterval(secondsPollingInterval, TimeUnit.SECONDS)
                    .until(isDeployedWorkerpool, Optional::isPresent);
        } catch (ConditionTimeoutException e) {
            log.error("Reached timeout when waiting for contract deployment"
                    + paramsPrinter, owner, name, e);
            return "";
        }
        return workerpoolAddress;
    }

    /**
     * Default method for creating workerpool
     *
     * @param name workerpool name
     * @return workerpool address (e.g.: 0x95ba540ca3c2dfd52a7e487a03e1358dfe9441ce)
     */
    public String createWorkerpool(String name) {
        return createWorkerpool(name, 10 * 60, 5);
    }
    // endregion

    // region app

    // TODO move app methods to its own class (e.g.: AppManager)
    public App getAppContract(String appAddress) {
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load App " +
                        "contract address " + appAddress);
        try {
            if (appAddress == null || appAddress.isEmpty()) {
                throw exceptionInInitializerError;
            }

            return App.load(appAddress,
                    web3jAbstractService.getWeb3j(),
                    credentials,
                    new DefaultGasProvider());
        } catch (Exception e) {
            log.error("Failed to load chainApp [address:{}]", appAddress, e);
        }
        return null;
    }

    public AppRegistry getAppRegistryContract(ContractGasProvider contractGasProvider) {
        String appRegistryAddress = "";
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load AppRegistry contract");
        try {
            appRegistryAddress = iexecHubContract.appregistry().send();
            if (appRegistryAddress == null || appRegistryAddress.isEmpty()) {
                throw exceptionInInitializerError;
            }
            return AppRegistry.load(
                    appRegistryAddress,
                    web3jAbstractService.getWeb3j(),
                    txManager,
                    contractGasProvider);
        } catch (Exception e) {
            log.error("Failed to load AppRegistry contract [address:{}]",
                    appRegistryAddress, e);
        }
        return null;
    }

    /**
     * This method allows to create a new app on iExec
     * <p>
     * Note: App is an ERC721. We use the Transfer event sent in the
     * ERC721 mint method to retrieve app address
     * tokenId is the generic form of appAddress
     *
     * @param name                   app name
     * @param multiAddress           app url
     * @param type                   app type
     * @param checksum               app sha256 checksum
     * @param mrEnclave              app mrEnclave
     * @param secondsTimeout         await app deployment for couple seconds
     * @param secondsPollingInterval check if app is deployed every couple seconds
     * @return app address (e.g.: 0x95ba540ca3c2dfd52a7e487a03e1358dfe9441ce)
     */
    public String createApp(String name, String multiAddress, String type,
                            String checksum, String mrEnclave, int secondsTimeout, int secondsPollingInterval) {
        String owner = credentials.getAddress();
        final String paramsPrinter = " [owner:{}, name:{}]";

        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(name)) {
            log.error("Non empty inputs are required" + paramsPrinter, owner, name);
            return "";
        }

        AppRegistry appRegistry =
                getAppRegistryContract(web3jAbstractService.getContractGasProvider());
        if (appRegistry == null) {
            log.error("Failed to get appRegistry" + paramsPrinter, owner, name);
            return "";
        }

        RemoteCall<TransactionReceipt> createAppCall = appRegistry
                .createApp(owner, name, type,
                        multiAddress.getBytes(StandardCharsets.UTF_8),
                        BytesUtils.hexStringToBytes32(checksum),
                        mrEnclave.getBytes(StandardCharsets.UTF_8));

        TransactionReceipt createAppReceipt;
        try {
            createAppReceipt = createAppCall.send();
        } catch (Exception e) {
            log.error("Failed to send createApp transaction" + paramsPrinter,
                    owner, name, e);
            return "";
        }

        if (!createAppReceipt.isStatusOK()) {
            log.error("Bad response status for createApp transaction" + paramsPrinter,
                    owner, name);
            return "";
        }

        String appAddress = getAssetAddressFromReceipt(createAppReceipt);

        if (StringUtils.isEmpty(appAddress)) {
            log.error("Failed to extract app address" + paramsPrinter,
                    owner, name);
            return "";
        }

        //tx hash can be null, manually verifying contract is deployed
        Callable<Optional<ChainApp>> isDeployedApp = () -> {
            log.info("Waiting for contract deployment" + paramsPrinter,
                    owner, name);
            return getChainApp(getAppContract(appAddress));
        };

        try {
            Awaitility.await()
                    .atMost(secondsTimeout, TimeUnit.SECONDS)
                    .pollInterval(secondsPollingInterval, TimeUnit.SECONDS)
                    .until(isDeployedApp, Optional::isPresent);
        } catch (ConditionTimeoutException e) {
            log.error("Reached timeout when waiting for contract deployment"
                    + paramsPrinter, owner, name, e);
            return "";
        }
        return appAddress;
    }

    /**
     * Default method for creating app
     *
     * @param name         app name
     * @param multiAddress app url
     * @param type         app type
     * @param checksum     app sha256 checksum
     * @param mrEnclave    app mrEnclave
     * @return app address (e.g.: 0x95ba540ca3c2dfd52a7e487a03e1358dfe9441ce)
     */
    public String createApp(String name, String multiAddress, String type,
                            String checksum, String mrEnclave) {
        return createApp(name, multiAddress, type, checksum, mrEnclave, 10 * 60, 5);
    }
    // endregion

    // region dataset

    // TODO move dataset methods to its own class (e.g.: DatasetManager)
    public Dataset getDatasetContract(String datasetAddress) {
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load Dataset " +
                        "contract address " + datasetAddress);
        try {
            if (datasetAddress == null || datasetAddress.isEmpty()) {
                throw exceptionInInitializerError;
            }

            return Dataset.load(datasetAddress,
                    web3jAbstractService.getWeb3j(),
                    credentials,
                    new DefaultGasProvider());
        } catch (Exception e) {
            log.error("Failed to load chainDataset [address:{}]", datasetAddress, e);
        }
        return null;
    }

    public DatasetRegistry getDatasetRegistryContract(ContractGasProvider contractGasProvider) {
        String datasetRegistryAddress = "";
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load DatasetRegistry contract");
        try {
            datasetRegistryAddress = iexecHubContract.datasetregistry().send();
            if (datasetRegistryAddress == null || datasetRegistryAddress.isEmpty()) {
                throw exceptionInInitializerError;
            }
            return DatasetRegistry.load(
                    datasetRegistryAddress,
                    web3jAbstractService.getWeb3j(),
                    txManager,
                    contractGasProvider);
        } catch (Exception e) {
            log.error("Failed to load DatasetRegistry contract [address:{}]",
                    datasetRegistryAddress, e);
        }
        return null;
    }

    /**
     * This method allows to create a new dataset on iExec
     * <p>
     * Note: Dataset is an ERC721. We use the Transfer event sent in the
     * ERC721 mint method to retrieve dataset address
     * tokenId is the generic form of datasetAddress
     *
     * @param name                   dataset name
     * @param multiAddress           dataset url
     * @param checksum               dataset sha256 checksum
     * @param secondsTimeout         await dataset deployment for couple seconds
     * @param secondsPollingInterval check if dataset is deployed every couple seconds
     * @return dataset address (e.g.: 0x95ba540ca3c2dfd52a7e487a03e1358dfe9441ce)
     */
    public String createDataset(String name, String multiAddress, String checksum,
                                int secondsTimeout, int secondsPollingInterval) {
        String owner = credentials.getAddress();
        final String paramsPrinter = " [owner:{}, name:{}, multiAddress:{}, checksum:{}]";

        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(name)
                || StringUtils.isEmpty(multiAddress) || StringUtils.isEmpty(checksum)) {
            log.error("Non empty inputs are required" + paramsPrinter,
                    owner, name, multiAddress, checksum);
            return "";
        }

        DatasetRegistry datasetRegistry =
                getDatasetRegistryContract(web3jAbstractService.getContractGasProvider());
        if (datasetRegistry == null) {
            log.error("Failed to get datasetRegistry" + paramsPrinter,
                    owner, name, multiAddress, checksum);
            return "";
        }

        RemoteCall<TransactionReceipt> createDatasetCall = datasetRegistry
                .createDataset(
                        owner,
                        name,
                        multiAddress.getBytes(StandardCharsets.UTF_8),
                        BytesUtils.hexStringToBytes32(checksum));

        TransactionReceipt createDatasetReceipt;
        try {
            createDatasetReceipt = createDatasetCall.send();
        } catch (Exception e) {
            log.error("Failed to send createDataset transaction" + paramsPrinter,
                    owner, name, multiAddress, checksum, e);
            return "";
        }

        if (!createDatasetReceipt.isStatusOK()) {
            log.error("Bad response status for createDataset transaction" + paramsPrinter,
                    owner, name, multiAddress, checksum);
            return "";
        }

        String datasetAddress = getAssetAddressFromReceipt(createDatasetReceipt);

        if (StringUtils.isEmpty(datasetAddress)) {
            log.error("Failed to extract dataset address" + paramsPrinter,
                    owner, name, multiAddress, checksum);
            return "";
        }

        //tx hash can be null, manually verifying contract is deployed
        Callable<Optional<ChainDataset>> isDeployedDataset = () -> {
            log.info("Waiting for contract deployment" + paramsPrinter,
                    owner, name, multiAddress, checksum);
            return getChainDataset(getDatasetContract(datasetAddress));
        };

        try {
            Awaitility.await()
                    .atMost(secondsTimeout, TimeUnit.SECONDS)
                    .pollInterval(secondsPollingInterval, TimeUnit.SECONDS)
                    .until(isDeployedDataset, Optional::isPresent);
        } catch (ConditionTimeoutException e) {
            log.error("Reached timeout when waiting for contract deployment"
                    + paramsPrinter, owner, name, multiAddress, checksum, e);
            return "";
        }
        return datasetAddress;
    }

    /**
     * Default method for creating dataset
     *
     * @param name         dataset name
     * @param multiAddress dataset url
     * @param checksum     dataset sha256 checksum
     * @return dataset address (e.g.: 0x95ba540ca3c2dfd52a7e487a03e1358dfe9441ce)
     */
    public String createDataset(String name, String multiAddress, String checksum) {
        return createDataset(name, multiAddress, checksum, 10 * 60, 5);
    }
    // endregion

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

            final ChainApp app = getChainApp(getAppContract(deal.app.pointer)).orElse(null);
            if (app == null) {
                return Optional.empty();
            }
            final ChainCategory category = getChainCategory(deal.category.longValue()).orElse(null);
            if (category == null) {
                return Optional.empty();
            }
            final ChainDataset dataset = getChainDataset(getDatasetContract(deal.dataset.pointer)).orElse(null);

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
     * Retrieves on-chain category with its blockchain ID
     * <p>
     * Note:
     * If `max execution time` is invalid, it is likely a blockchain issue.
     * In this case, in order to protect workflows based on top of it, the category
     * won't be accessible from this method
     *
     * @param id blockchain ID of the category (e.g: 0x123..abc)
     * @return category object
     */
    public Optional<ChainCategory> getChainCategory(long id) {
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
                return Optional.empty();
            }
            return Optional.of(chainCategory);
        } catch (Exception e) {
            log.error("Failed to get ChainCategory [id:{}]", id, e);
        }
        return Optional.empty();
    }

    public Optional<ChainWorkerpool> getChainWorkerpool(Workerpool workerpool) {
        if (workerpool != null && !workerpool.getContractAddress().equals(BytesUtils.EMPTY_ADDRESS)) {
            try {
                return Optional.of(ChainWorkerpool.builder()
                        .chainWorkerpoolId(workerpool.getContractAddress())
                        .owner(workerpool.owner().send())
                        .description(workerpool.m_workerpoolDescription().send())
                        .build());
            } catch (Exception e) {
                log.error("Failed to get ChainDataset [chainDatasetId:{}]",
                        workerpool.getContractAddress(), e);
            }
        }
        return Optional.empty();
    }

    public Optional<ChainApp> getChainApp(App app) {
        if (app == null ||
                StringUtils.isEmpty(app.getContractAddress()) ||
                app.getContractAddress().equals(BytesUtils.EMPTY_ADDRESS)) {
            return Optional.empty();
        }
        ChainApp.ChainAppBuilder chainAppBuilder = ChainApp.builder();
        try {
            chainAppBuilder
                    .chainAppId(app.getContractAddress())
                    .name(app.m_appName().send())
                    .type(app.m_appType().send())
                    .uri(BytesUtils.bytesToString(app.m_appMultiaddr().send()))
                    .checksum(BytesUtils.bytesToString(app.m_appChecksum().send()));
        } catch (Exception e) {
            log.error("Failed to get chain app [chainAppId:{}]",
                    app.getContractAddress(), e);
            return Optional.empty();
        }
        String mrEnclave;
        try {
            mrEnclave = new String(app.m_appMREnclave().send());
        } catch (Exception e) {
            log.error("Failed to get chain app mrenclave [chainAppId:{}]",
                    app.getContractAddress(), e);
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
                    app.getContractAddress(), mrEnclave, e);
            return Optional.empty();
        }
        return Optional.of(chainAppBuilder.build());
    }

    public Optional<ChainDataset> getChainDataset(Dataset dataset) {
        if (dataset != null && !dataset.getContractAddress().equals(BytesUtils.EMPTY_ADDRESS)) {
            try {
                return Optional.of(ChainDataset.builder()
                        .chainDatasetId(dataset.getContractAddress())
                        .owner(dataset.owner().send())
                        .name(dataset.m_datasetName().send())
                        .uri(BytesUtils.bytesToString(dataset.m_datasetMultiaddr().send()))
                        .checksum(BytesUtils.bytesToString(dataset.m_datasetChecksum().send()))
                        .build());
            } catch (Exception e) {
                log.error("Failed to get ChainDataset [chainDatasetId:{}]",
                        dataset.getContractAddress(), e);
            }
        }
        return Optional.empty();
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

    public Ownable getOwnableContract(String address) {
        ExceptionInInitializerError exceptionInInitializerError =
                new ExceptionInInitializerError("Failed to load Ownable " +
                        "contract " + address);
        try {
            if (address == null || address.isEmpty()) {
                throw exceptionInInitializerError;
            }

            return Ownable.load(address,
                    web3jAbstractService.getWeb3j(),
                    credentials,
                    new DefaultGasProvider());
        } catch (Exception e) {
            log.error("Failed to load Ownable [address:{}]", address, e);
        }
        return null;
    }

    public String getOwner(String address) {
        Ownable ownableContract = getOwnableContract(address);

        if (ownableContract != null) {
            try {
                return ownableContract.owner().send();
            } catch (Exception e) {
                log.error("Failed to get owner [address:{}]", address, e);
            }
        }
        return "";
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
            maxNbOfPeriodsForConsensus = iexecHubContract.contribution_deadline_ratio().send().longValue();
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
        // Magical non-null retry delay to ease testing and because there are no retries
        final TaskDescription taskDescription = repeatGetTaskDescriptionFromChain(chainTaskId, 1000, 0)
                .orElse(null);

        if (taskDescription == null) {
            log.error("Couldn't get task description from chain [chainTaskId:{}]",
                    chainTaskId);
            return false;
        }

        return taskDescription.isTeeTask();
    }

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
