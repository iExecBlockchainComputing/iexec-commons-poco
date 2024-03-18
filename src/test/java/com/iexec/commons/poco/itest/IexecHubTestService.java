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
import com.iexec.commons.poco.chain.Web3jAbstractService;
import com.iexec.commons.poco.encoding.AssetDataEncoder;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

@Slf4j
public class IexecHubTestService extends IexecHubAbstractService {
    static final BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
    static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);
    static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";

    private static final String APP_REGISTRY_SELECTOR = "0x45b637a9";
    private static final String DATASET_REGISTRY_SELECTOR = "0xb1b11d2c";
    private static final String WORKERPOOL_REGISTRY_SELECTOR = "0x90a0f546";

    private final SignerService signerService;

    public IexecHubTestService(Credentials credentials, Web3jAbstractService web3jAbstractService) {
        super(credentials, web3jAbstractService, IEXEC_HUB_ADDRESS);
        signerService = new SignerService(
                web3jAbstractService.getWeb3j(), web3jAbstractService.getChainId(), credentials);
    }

    public String callCreateApp(String name) throws IOException {
        log.info("callCreateApp");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, APP_REGISTRY_SELECTOR));
        final String appTxData = AssetDataEncoder.encodeApp(
                signerService.getAddress(),
                name,
                "DOCKER",
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64),
                "{}"
        );
        return toEthereumAddress(signerService.sendCall(appRegistryAddress, appTxData));
    }

    public String submitCreateAppTx(BigInteger nonce, String name) throws IOException {
        log.info("submitCreateAppTx");
        final String appRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, APP_REGISTRY_SELECTOR));
        log.info("app registry address {}", appRegistryAddress);
        final String appTxData = AssetDataEncoder.encodeApp(
                signerService.getAddress(),
                name,
                "DOCKER",
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64),
                "{}"
        );
        final String appTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, appRegistryAddress, appTxData
        );
        log.info("app tx hash {}", appTxHash);
        return appTxHash;
    }

    public String callCreateDataset(String name) throws IOException {
        log.info("callCreateDataset");
        final String datasetRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, DATASET_REGISTRY_SELECTOR));
        final String datasetTxData = AssetDataEncoder.encodeDataset(
                signerService.getAddress(),
                name,
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64)
        );
        return toEthereumAddress(signerService.sendCall(datasetRegistryAddress, datasetTxData));
    }

    public String submitCreateDatasetTx(BigInteger nonce, String name) throws IOException {
        log.info("submitCreateDatasetTx");
        final String datasetRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, DATASET_REGISTRY_SELECTOR));
        log.info("dataset registry address {}", datasetRegistryAddress);
        final String datasetTxData = AssetDataEncoder.encodeDataset(
                signerService.getAddress(),
                name,
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64)
        );
        final String datasetTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, datasetRegistryAddress, datasetTxData
        );
        log.info("dataset tx hash {}", datasetTxHash);
        return datasetTxHash;
    }

    public String callCreateWorkerpool(String name) throws Exception {
        log.info("callCreateWorkerpool");
        final String workerpoolRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, WORKERPOOL_REGISTRY_SELECTOR));
        final String workerpoolTxData = AssetDataEncoder.encodeWorkerpool(
                signerService.getAddress(),
                name
        );
        return toEthereumAddress(signerService.sendCall(workerpoolRegistryAddress, workerpoolTxData));
    }

    public String submitCreateWorkerpoolTx(BigInteger nonce, String name) throws IOException {
        log.info("submitCreateWorkerpoolTx");
        final String workerpoolRegistryAddress = toEthereumAddress(
                signerService.sendCall(IEXEC_HUB_ADDRESS, WORKERPOOL_REGISTRY_SELECTOR));
        log.info("workerpool registry address {}", workerpoolRegistryAddress);
        final String workerpoolTxData = AssetDataEncoder.encodeWorkerpool(
                signerService.getAddress(),
                name
        );
        final String workerpoolTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, workerpoolRegistryAddress, workerpoolTxData
        );
        log.info("workerpool tx hash {}", workerpoolTxHash);
        return workerpoolTxHash;
    }

    private String toEthereumAddress(String hexaString) {
        return Numeric.toHexStringWithPrefixZeroPadded(
                Numeric.toBigInt(hexaString), 40);
    }
}
