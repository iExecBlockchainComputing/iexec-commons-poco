/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.encoding.AssetDataEncoder;
import lombok.extern.slf4j.Slf4j;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

@Slf4j
public abstract class AbstractAssetDeploymentService {
    protected final SignerService signerService;

    private final String assetRegistrySelector;
    private String assetRegistryAddress;

    protected AbstractAssetDeploymentService(SignerService signerService, String assetRegistrySelector) {
        this.signerService = signerService;
        this.assetRegistrySelector = assetRegistrySelector;
    }

    public void initRegistryAddress(String iexecHubAddress) throws IOException {
        if (this.assetRegistryAddress != null) {
            return;
        }
        this.assetRegistryAddress = toEthereumAddress(signerService.sendCall(iexecHubAddress, assetRegistrySelector));
    }

    public String callCreateAsset(String assetTxData) throws IOException {
        return toEthereumAddress(signerService.sendCall(assetRegistryAddress, assetTxData));
    }

    public boolean isAssetDeployed(String address) throws IOException {
        final String isRegisteredTxData = AssetDataEncoder.encodeIsRegistered(address);
        return Numeric.toBigInt(signerService.sendCall(assetRegistryAddress, isRegisteredTxData)).equals(BigInteger.ONE);
    }

    public BigInteger estimateCreateAsset(String assetTxData) throws IOException {
        return signerService.estimateGas(assetRegistryAddress, assetTxData);
    }

    public String submitAssetTxData(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String assetTxData) throws IOException {
        return signerService.signAndSendTransaction(nonce, gasPrice, gasLimit, assetRegistryAddress, assetTxData);
    }

    private String toEthereumAddress(String hexaString) {
        return Numeric.toHexStringWithPrefixZeroPadded(
                Numeric.toBigInt(hexaString), 40);
    }
}
