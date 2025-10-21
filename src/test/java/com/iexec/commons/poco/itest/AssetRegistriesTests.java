/*
 * Copyright 2024-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.ChainApp;
import com.iexec.commons.poco.chain.ChainDataset;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.exceptions.JsonRpcError;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static com.iexec.commons.poco.itest.IexecHubTestService.ASSET_CHECKSUM;
import static com.iexec.commons.poco.itest.IexecHubTestService.ASSET_MULTI_ADDRESS;
import static com.iexec.commons.poco.itest.Web3jTestService.MINING_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("itest")
@Testcontainers
class AssetRegistriesTests {

    private SignerService signerService;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;

    @Container
    static ComposeContainer environment = new ComposeContainer(new File("docker-compose.yml"))
            .withPull(true)
            .withExposedService(SERVICE_NAME, SERVICE_PORT);

    @BeforeEach
    void init() throws CipherException, IOException {
        final Credentials credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        String chainNodeAddress = "http://" + environment.getServiceHost(SERVICE_NAME, SERVICE_PORT) + ":" +
                environment.getServicePort(SERVICE_NAME, SERVICE_PORT);
        web3jService = new Web3jTestService(chainNodeAddress);
        iexecHubService = new IexecHubTestService(credentials, web3jService);
        signerService = new SignerService(web3jService.getWeb3j(), web3jService.getChainId(), credentials);
    }

    @Test
    void shouldCreateAndPredictCallsBeEqualWhenAssetNotDeployed() {
        final String appName = RandomStringUtils.randomAlphanumeric(16);
        final String datasetName = RandomStringUtils.randomAlphanumeric(16);
        final String workerpoolName = RandomStringUtils.randomAlphanumeric(16);

        assertAll(
                () -> assertThat(iexecHubService.callCreateApp(appName))
                        .isEqualTo(iexecHubService.callPredictApp(appName)),
                () -> assertThat(iexecHubService.callCreateDataset(datasetName))
                        .isEqualTo(iexecHubService.callPredictDataset(datasetName)),
                () -> assertThat(iexecHubService.callCreateWorkerpool(workerpoolName))
                        .isEqualTo(iexecHubService.callPredictWorkerpool(workerpoolName))
        );
    }

    @Test
    void shouldCreateCallRevertWhenAssetDeployed() throws IOException {
        final String appName = RandomStringUtils.randomAlphanumeric(16);
        final String datasetName = RandomStringUtils.randomAlphanumeric(16);
        final String workerpoolName = RandomStringUtils.randomAlphanumeric(16);
        BigInteger nonce = signerService.getNonce();
        final String appTxHash = iexecHubService.submitCreateAppTx(nonce, appName);
        nonce = nonce.add(BigInteger.ONE);
        final String datasetTxHash = iexecHubService.submitCreateDatasetTx(nonce, datasetName);
        nonce = nonce.add(BigInteger.ONE);
        final String workerpoolTxHash = iexecHubService.submitCreateWorkerpoolTx(nonce, workerpoolName);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(appTxHash, datasetTxHash, workerpoolTxHash));
        assertThat(web3jService.areTxStatusOK(appTxHash, datasetTxHash, workerpoolTxHash)).isTrue();

        // fetch asset addresses from call on predict assets
        final String predictedAppAddress = iexecHubService.callPredictApp(appName);
        final String predictedDatasetAddress = iexecHubService.callPredictDataset(datasetName);
        final String predictedWorkerpoolAddress = iexecHubService.callPredictWorkerpool(workerpoolName);

        // check assets are deployed
        assertAll(
                () -> assertThat(iexecHubService.isAppPresent(predictedAppAddress)).isTrue(),
                () -> assertThat(iexecHubService.isAppPresent(predictedDatasetAddress)).isFalse(),
                () -> assertThat(iexecHubService.isAppPresent(predictedWorkerpoolAddress)).isFalse(),
                () -> assertThat(iexecHubService.isDatasetPresent(predictedAppAddress)).isFalse(),
                () -> assertThat(iexecHubService.isDatasetPresent(predictedDatasetAddress)).isTrue(),
                () -> assertThat(iexecHubService.isDatasetPresent(predictedWorkerpoolAddress)).isFalse(),
                () -> assertThat(iexecHubService.isWorkerpoolPresent(predictedAppAddress)).isFalse(),
                () -> assertThat(iexecHubService.isWorkerpoolPresent(predictedDatasetAddress)).isFalse(),
                () -> assertThat(iexecHubService.isWorkerpoolPresent(predictedWorkerpoolAddress)).isTrue()
        );

        // call on create assets should revert
        final String errorMessage = "Create2: Failed on deploy";

        assertAll(
                () -> assertThatThrownBy(() -> iexecHubService.callCreateApp(appName), "Should have failed to call createApp")
                        .isInstanceOf(JsonRpcError.class)
                        .hasMessage(errorMessage),
                () -> assertThatThrownBy(() -> iexecHubService.callCreateDataset(datasetName), "Should have failed to call createDataset")
                        .isInstanceOf(JsonRpcError.class)
                        .hasMessage(errorMessage),
                () -> assertThatThrownBy(() -> iexecHubService.callCreateWorkerpool(workerpoolName), "Should have failed to call createWorkerpool")
                        .isInstanceOf(JsonRpcError.class)
                        .hasMessage(errorMessage)
        );

        final Optional<ChainApp> chainApp = iexecHubService.getChainApp(predictedAppAddress);
        assertThat(chainApp).contains(
                ChainApp.builder()
                        .chainAppId(predictedAppAddress)
                        .checksum("0x" + ASSET_CHECKSUM)
                        .multiaddr(ASSET_MULTI_ADDRESS)
                        .enclaveConfiguration(TeeEnclaveConfiguration.buildEnclaveConfigurationFromJsonString("{}"))
                        .type("DOCKER")
                        .build()
        );
        final Optional<ChainDataset> chainDataset = iexecHubService.getChainDataset(predictedDatasetAddress);
        assertThat(chainDataset).contains(
                ChainDataset.builder()
                        .chainDatasetId(predictedDatasetAddress)
                        .checksum("0x" + ASSET_CHECKSUM)
                        .multiaddr(ASSET_MULTI_ADDRESS)
                        .build()
        );
    }
}
