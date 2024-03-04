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

import java.math.BigInteger;

@Slf4j
public class IexecHubTestService extends IexecHubAbstractService {
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);
    private static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";

    private final SignerService signerService;

    public IexecHubTestService(Credentials credentials, Web3jAbstractService web3jAbstractService) {
        super(credentials, web3jAbstractService, IEXEC_HUB_ADDRESS);
        signerService = new SignerService(
                web3jAbstractService.getWeb3j(), web3jAbstractService.getChainId(), credentials);
    }

    public String submitCreateAppTx(BigInteger nonce, String name) throws Exception {
        String appRegistryAddress = getHubContract().appregistry().send();
        String appTxData = AssetDataEncoder.encodeApp(
                signerService.getAddress(),
                name,
                "DOCKER",
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64),
                "{}"
        );
        String appTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, appRegistryAddress, appTxData
        );
        log.info("app tx hash {}", appTxHash);
        return appTxHash;
    }

    public String submitCreateDatasetTx(BigInteger nonce, String name) throws Exception {
        String datasetRegistryAddress = getHubContract().datasetregistry().send();
        String datasetTxData = AssetDataEncoder.encodeDataset(
                signerService.getAddress(),
                name,
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64)
        );
        String datasetTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, datasetRegistryAddress, datasetTxData
        );
        log.info("dataset tx hash {}", datasetTxHash);
        return datasetTxHash;
    }

    public String submitCreateWorkerpoolTx(BigInteger nonce, String name) throws Exception {
        String workerpoolRegistryAddress = getHubContract().workerpoolregistry().send();
        String workerpoolTxData = AssetDataEncoder.encodeWorkerpool(
                signerService.getAddress(),
                name
        );
        String workerpoolTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, workerpoolRegistryAddress, workerpoolTxData
        );
        log.info("workerpool tx hash {}", workerpoolTxHash);
        return workerpoolTxHash;
    }
}
