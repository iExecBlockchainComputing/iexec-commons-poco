/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.itest;

import com.iexec.commons.poco.chain.IexecHubAbstractService;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.encoding.AssetDataEncoder;
import com.iexec.commons.poco.encoding.LogTopic;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.OrdersService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.MINING_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
public class IexecHubTestService extends IexecHubAbstractService {
    static final BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
    static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);
    static final long CHAIN_ID = 65535L;
    static final String IEXEC_HUB_ADDRESS = "0xc4b11f41746D3Ad8504da5B383E1aB9aa969AbC7";

    static final String ASSET_MULTI_ADDRESS = "multiAddress";
    static final String ASSET_CHECKSUM = Numeric.toHexStringNoPrefix(new byte[32]);

    private static final String APP_REGISTRY_SELECTOR = "0x45b637a9";
    private static final String DATASET_REGISTRY_SELECTOR = "0xb1b11d2c";
    private static final String WORKERPOOL_REGISTRY_SELECTOR = "0x90a0f546";

    private final String ownerAddress;
    private final SignerService signerService;
    private final Web3jTestService web3jTestService;

    private final AssetDeploymentService appDeploymentService;
    private final AssetDeploymentService datasetDeploymentService;
    private final AssetDeploymentService workerpoolDeploymentService;

    public IexecHubTestService(Credentials credentials, Web3jTestService web3jTestService) throws IOException {
        super(credentials, web3jTestService, IEXEC_HUB_ADDRESS);
        this.signerService = new SignerService(
                web3jTestService.getWeb3j(), web3jTestService.getChainId(), credentials);
        this.ownerAddress = credentials.getAddress();
        this.web3jTestService = web3jTestService;
        this.appDeploymentService = new AssetDeploymentService(signerService, APP_REGISTRY_SELECTOR);
        this.datasetDeploymentService = new AssetDeploymentService(signerService, DATASET_REGISTRY_SELECTOR);
        this.workerpoolDeploymentService = new AssetDeploymentService(signerService, WORKERPOOL_REGISTRY_SELECTOR);
        appDeploymentService.initRegistryAddress(IEXEC_HUB_ADDRESS);
        datasetDeploymentService.initRegistryAddress(IEXEC_HUB_ADDRESS);
        workerpoolDeploymentService.initRegistryAddress(IEXEC_HUB_ADDRESS);
    }

    public Map<String, String> deployAssets() throws IOException {
        final String predictedAppAddress = callPredictApp(APP_NAME);
        final String predictedDatasetAddress = callPredictDataset(DATASET_NAME);
        final String predictedWorkerpoolAddress = callPredictWorkerpool(WORKERPOOL_NAME);

        BigInteger nonce = signerService.getNonce();
        final List<String> txHashes = new ArrayList<>();
        if (!isAppPresent(predictedAppAddress)) {
            txHashes.add(submitCreateAppTx(nonce, APP_NAME));
            nonce = nonce.add(BigInteger.ONE);
        }
        if (!isDatasetPresent(predictedDatasetAddress)) {
            txHashes.add(submitCreateDatasetTx(nonce, DATASET_NAME));
            nonce = nonce.add(BigInteger.ONE);
        }
        if (!isWorkerpoolPresent(predictedWorkerpoolAddress)) {
            txHashes.add(submitCreateWorkerpoolTx(nonce, WORKERPOOL_NAME));
        }

        // Wait for assets deployment to be able to call MatchOrders
        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jTestService.areTxMined(txHashes.toArray(String[]::new)));

        // check
        if (!txHashes.isEmpty()) {
            assertThat(web3jTestService.getDeployedAssets(txHashes.toArray(String[]::new)))
                    .containsExactly(predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress);
            for (final String txHash : txHashes) {
                assertThat(fetchLogTopics(txHash)).isEqualTo(List.of("Transfer"));
            }
        }

        return Map.of(
                "app", predictedAppAddress,
                "dataset", predictedDatasetAddress,
                "workerpool", predictedWorkerpoolAddress);
    }

    // region createApp
    public String callCreateApp(String name) throws IOException {
        log.info("callCreateApp");
        final String appTxData = createAppTxData(name);
        return appDeploymentService.callCreateAsset(appTxData);
    }

    public String callPredictApp(String name) throws IOException {
        log.info("callPredictApp");
        final String appTxData = AssetDataEncoder.encodePredictApp(
                ownerAddress, name, "DOCKER", ASSET_MULTI_ADDRESS, ASSET_CHECKSUM, "{}");
        return appDeploymentService.callCreateAsset(appTxData);
    }

    public String submitCreateAppTx(BigInteger nonce, String name) throws IOException {
        log.info("submitCreateAppTx");
        final String appTxData = createAppTxData(name);
        final String appTxHash = appDeploymentService.submitAssetTxData(nonce, GAS_PRICE, appTxData);
        log.info("app tx hash {}", appTxHash);
        return appTxHash;
    }

    private String createAppTxData(String name) {
        return AssetDataEncoder.encodeApp(
                ownerAddress,
                name,
                "DOCKER",
                ASSET_MULTI_ADDRESS,
                ASSET_CHECKSUM,
                "{}"
        );
    }

    public boolean isAppPresent(String address) throws IOException {
        log.info("isAppPresent");
        return appDeploymentService.isAssetDeployed(address);
    }
    // endregion

    // region createDataset
    public String callCreateDataset(String name) throws IOException {
        log.info("callCreateDataset");
        final String datasetTxData = createDatasetTxData(name);
        return datasetDeploymentService.callCreateAsset(datasetTxData);
    }

    public String callPredictDataset(String name) throws IOException {
        log.info("callPredictDataset");
        final String datasetTxData = AssetDataEncoder.encodePredictDataset(ownerAddress, name, ASSET_MULTI_ADDRESS, ASSET_CHECKSUM);
        return datasetDeploymentService.callCreateAsset(datasetTxData);
    }

    public String submitCreateDatasetTx(BigInteger nonce, String name) throws IOException {
        log.info("submitCreateDatasetTx");
        final String datasetTxData = createDatasetTxData(name);
        final String datasetTxHash = datasetDeploymentService.submitAssetTxData(nonce, GAS_PRICE, datasetTxData);
        log.info("dataset tx hash {}", datasetTxHash);
        return datasetTxHash;
    }

    private String createDatasetTxData(String name) {
        return AssetDataEncoder.encodeDataset(
                ownerAddress,
                name,
                ASSET_MULTI_ADDRESS,
                ASSET_CHECKSUM
        );
    }

    public boolean isDatasetPresent(String address) throws IOException {
        log.info("isDatasetPresent");
        return datasetDeploymentService.isAssetDeployed(address);
    }
    // endregion

    // region createWorkerpool
    public String callCreateWorkerpool(String name) throws IOException {
        log.info("callCreateWorkerpool");
        final String workerpoolTxData = createWorkerpoolTxData(name);
        return workerpoolDeploymentService.callCreateAsset(workerpoolTxData);
    }

    public String callPredictWorkerpool(String name) throws IOException {
        log.info("callPredictWorkerpool");
        final String workerpoolTxData = AssetDataEncoder.encodePredictWorkerpool(ownerAddress, name);
        return workerpoolDeploymentService.callCreateAsset(workerpoolTxData);
    }

    public String submitCreateWorkerpoolTx(BigInteger nonce, String name) throws IOException {
        log.info("submitCreateWorkerpoolTx");
        final String workerpoolTxData = createWorkerpoolTxData(name);
        final String workerpoolTxHash = workerpoolDeploymentService.submitAssetTxData(nonce, GAS_PRICE, workerpoolTxData);
        log.info("workerpool tx hash {}", workerpoolTxHash);
        return workerpoolTxHash;
    }

    private String createWorkerpoolTxData(String name) {
        return AssetDataEncoder.encodeWorkerpool(
                ownerAddress,
                name
        );
    }

    public boolean isWorkerpoolPresent(String address) throws IOException {
        log.info("isWorkerpoolPresent");
        return workerpoolDeploymentService.isAssetDeployed(address);
    }
    // endregion

    public List<String> fetchLogTopics(String txHash) {
        return web3jTestService.getTransactionReceipt(txHash)
                .getLogs()
                .stream()
                .map(log -> log.getTopics().get(0))
                .map(LogTopic::decode)
                .toList();
    }
}
