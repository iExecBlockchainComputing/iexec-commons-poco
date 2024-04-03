/*
 * Copyright 2023-2024 IEXEC BLOCKCHAIN TECH
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class IexecHubTestService extends IexecHubAbstractService {
    static final BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
    static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);
    static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";

    private static final String APP_REGISTRY_SELECTOR = "0x45b637a9";
    private static final String DATASET_REGISTRY_SELECTOR = "0xb1b11d2c";
    private static final String WORKERPOOL_REGISTRY_SELECTOR = "0x90a0f546";

    private final SignerService signerService;
    private final Web3jTestService web3jTestService;

    public IexecHubTestService(Credentials credentials, Web3jTestService web3jTestService) {
        super(credentials, web3jTestService, IEXEC_HUB_ADDRESS);
        signerService = new SignerService(
                web3jTestService.getWeb3j(), web3jTestService.getChainId(), credentials);
        this.web3jTestService = web3jTestService;
    }

    // region createApp
    public String callCreateApp(String name) throws IOException {
        log.info("callCreateApp");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, APP_REGISTRY_SELECTOR));
        final String appTxData = createAppTxData(name);
        return toEthereumAddress(signerService.sendCall(appRegistryAddress, appTxData));
    }

    public BigInteger estimateCreateApp(String name) throws IOException {
        log.info("estimateCreateApp");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, APP_REGISTRY_SELECTOR));
        final String appTxData = createAppTxData(name);
        return signerService.estimateGas(appRegistryAddress, appTxData);
    }

    public String submitCreateAppTx(BigInteger nonce, BigInteger gasLimit, String name) throws IOException {
        log.info("submitCreateAppTx");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, APP_REGISTRY_SELECTOR));
        log.info("app registry address {}", appRegistryAddress);
        final String appTxData = createAppTxData(name);
        final String appTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, gasLimit, appRegistryAddress, appTxData
        );
        log.info("app tx hash {}", appTxHash);
        return appTxHash;
    }

    private String createAppTxData(String name) {
        return AssetDataEncoder.encodeApp(
                signerService.getAddress(),
                name,
                "DOCKER",
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64),
                "{}"
        );
    }

    public boolean isAppPresent(String address) throws IOException {
        log.info("isAppPresent");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, APP_REGISTRY_SELECTOR));
        final String appTxData = AssetDataEncoder.encodeIsRegistered(address);
        return Numeric.toBigInt(signerService.sendCall(appRegistryAddress, appTxData)).equals(BigInteger.ONE);
    }
    // endregion

    // region createDataset
    public String callCreateDataset(String name) throws IOException {
        log.info("callCreateDataset");
        final String datasetRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, DATASET_REGISTRY_SELECTOR));
        final String datasetTxData = createDatasetTxData(name);
        return toEthereumAddress(signerService.sendCall(datasetRegistryAddress, datasetTxData));
    }

    public BigInteger estimateCreateDataset(String name) throws IOException {
        log.info("callCreateDataset");
        final String datasetRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, DATASET_REGISTRY_SELECTOR));
        final String datasetTxData = createDatasetTxData(name);
        return signerService.estimateGas(datasetRegistryAddress, datasetTxData);
    }

    public String submitCreateDatasetTx(BigInteger nonce, BigInteger gasLimit, String name) throws IOException {
        log.info("submitCreateDatasetTx");
        final String datasetRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, DATASET_REGISTRY_SELECTOR));
        log.info("dataset registry address {}", datasetRegistryAddress);
        final String datasetTxData = createDatasetTxData(name);
        final String datasetTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, gasLimit, datasetRegistryAddress, datasetTxData
        );
        log.info("dataset tx hash {}", datasetTxHash);
        return datasetTxHash;
    }

    private String createDatasetTxData(String name) {
        return AssetDataEncoder.encodeDataset(
                signerService.getAddress(),
                name,
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64)
        );
    }

    public boolean isDatasetPresent(String address) throws IOException {
        log.info("isDatasetPresent");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, DATASET_REGISTRY_SELECTOR));
        final String appTxData = AssetDataEncoder.encodeIsRegistered(address);
        return Numeric.toBigInt(signerService.sendCall(appRegistryAddress, appTxData)).equals(BigInteger.ONE);
    }
    // endregion

    // region createWorkerpool
    public String callCreateWorkerpool(String name) throws Exception {
        log.info("callCreateWorkerpool");
        final String workerpoolRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, WORKERPOOL_REGISTRY_SELECTOR));
        final String workerpoolTxData = createWorkerpoolTxData(name);
        return toEthereumAddress(signerService.sendCall(workerpoolRegistryAddress, workerpoolTxData));
    }

    public BigInteger estimateCreateWorkerpool(String name) throws Exception {
        log.info("callCreateWorkerpool");
        final String workerpoolRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, WORKERPOOL_REGISTRY_SELECTOR));
        final String workerpoolTxData = createWorkerpoolTxData(name);
        return signerService.estimateGas(workerpoolRegistryAddress, workerpoolTxData);
    }

    public String submitCreateWorkerpoolTx(BigInteger nonce, BigInteger gasLimit, String name) throws IOException {
        log.info("submitCreateWorkerpoolTx");
        final String workerpoolRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, WORKERPOOL_REGISTRY_SELECTOR));
        log.info("workerpool registry address {}", workerpoolRegistryAddress);
        final String workerpoolTxData = createWorkerpoolTxData(name);
        final String workerpoolTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, gasLimit, workerpoolRegistryAddress, workerpoolTxData
        );
        log.info("workerpool tx hash {}", workerpoolTxHash);
        return workerpoolTxHash;
    }

    private String createWorkerpoolTxData(String name) {
        return AssetDataEncoder.encodeWorkerpool(
                signerService.getAddress(),
                name
        );
    }

    public boolean isWorkerpoolPresent(String address) throws IOException {
        log.info("isWorkerpoolPresent");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, WORKERPOOL_REGISTRY_SELECTOR));
        final String appTxData = AssetDataEncoder.encodeIsRegistered(address);
        return Numeric.toBigInt(signerService.sendCall(appRegistryAddress, appTxData)).equals(BigInteger.ONE);
    }
    // endregion

    public List<String> fetchLogTopics(String txHash) {
        return web3jTestService.getTransactionReceipt(txHash)
                .getLogs()
                .stream()
                .map(log -> log.getTopics().get(0))
                .map(LogTopic::decode)
                .collect(Collectors.toList());
    }

    private String toEthereumAddress(String hexaString) {
        return Numeric.toHexStringWithPrefixZeroPadded(
                Numeric.toBigInt(hexaString), 40);
    }
}
