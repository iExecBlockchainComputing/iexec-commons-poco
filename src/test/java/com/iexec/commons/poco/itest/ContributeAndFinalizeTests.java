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

import com.iexec.commons.poco.chain.ChainUtils;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.encoding.PoCoDataEncoder;
import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static com.iexec.commons.poco.itest.IexecHubTestService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.MINING_TIMEOUT;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@Tag("itest")
@Testcontainers
class ContributeAndFinalizeTests {

    private IexecHubTestService iexecHubService;
    private OrdersService ordersService;
    private SignerService signerService;
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
        ordersService = new OrdersService(signerService);
    }

    @Test
    void shouldContributeAndFinalize() throws Exception {
        final Map<String, String> assetAddresses = iexecHubService.deployAssets();
        final String predictedAppAddress = assetAddresses.get("app");
        final String predictedDatasetAddress = assetAddresses.get("dataset");
        final String predictedWorkerpoolAddress = assetAddresses.get("workerpool");

        final OrdersService.DealOrders dealOrders = ordersService.buildAllSignedOrders(
                predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress, BigInteger.ONE);
        final String predictedDealId = ordersService.callMatchOrders(dealOrders);
        final String predictedChainTaskId = ChainUtils.generateChainTaskId(predictedDealId, 0);
        BigInteger nonce = signerService.getNonce();
        final String matchOrdersTxHash = ordersService.submitMatchOrders(dealOrders, nonce);

        // init
        final String initializeTxData = PoCoDataEncoder.encodeInitialize(predictedDealId, 0);
        nonce = nonce.add(BigInteger.ONE);
        final String initializeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, initializeTxData);

        // enclave challenge
        final SignerService workerSigner = web3jService.createSigner();
        final SignerService enclaveSigner = new SignerService(web3jService.getWeb3j(), web3jService.getChainId());
        final String enclaveChallenge = enclaveSigner.getAddress();

        final String resultDigest = TeeUtils.TEE_SCONE_ONLY_TAG;
        final String resultHash = HashUtils.concatenateAndHash(predictedChainTaskId, resultDigest);
        final String resultSeal = HashUtils.concatenateAndHash(workerSigner.getAddress(), predictedChainTaskId, resultDigest);
        final String enclaveHash = HashUtils.concatenateAndHash(resultHash, resultSeal);
        final String enclaveSignature = enclaveSigner.signMessageHash(enclaveHash).getValue();
        final String authorizationHash = HashUtils.concatenateAndHash(workerSigner.getAddress(), predictedChainTaskId, enclaveChallenge);
        final String authorizationSignature = signerService.signMessageHash(authorizationHash).getValue();

        // contributeAndFinalize
        final String contributeAndFinalizeTxData = PoCoDataEncoder.encodeContributeAndFinalize(
                predictedChainTaskId, resultDigest, new byte[0], new byte[0], enclaveChallenge, enclaveSignature, authorizationSignature);
        final String contributeAndFinalizeTxHash = workerSigner.signAndSendTransaction(
                BigInteger.ZERO, BigInteger.ZERO, GAS_LIMIT, IEXEC_HUB_ADDRESS, contributeAndFinalizeTxData);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(matchOrdersTxHash, initializeTxHash, contributeAndFinalizeTxHash));
        assertThat(web3jService.areTxStatusOK(matchOrdersTxHash, initializeTxHash, contributeAndFinalizeTxHash)).isTrue();

        // checks
        assertThat(iexecHubService.getChainDeal(predictedDealId)).isPresent();
        assertThat(iexecHubService.getChainTask(predictedChainTaskId)).isPresent();

        // Assets deployment
        assertThat(iexecHubService.isAppPresent(predictedAppAddress)).isTrue();
        assertThat(iexecHubService.isAppPresent(predictedDatasetAddress)).isFalse();
        assertThat(iexecHubService.isAppPresent(predictedWorkerpoolAddress)).isFalse();
        assertThat(iexecHubService.isDatasetPresent(predictedAppAddress)).isFalse();
        assertThat(iexecHubService.isDatasetPresent(predictedDatasetAddress)).isTrue();
        assertThat(iexecHubService.isDatasetPresent(predictedWorkerpoolAddress)).isFalse();
        assertThat(iexecHubService.isWorkerpoolPresent(predictedAppAddress)).isFalse();
        assertThat(iexecHubService.isWorkerpoolPresent(predictedDatasetAddress)).isFalse();
        assertThat(iexecHubService.isWorkerpoolPresent(predictedWorkerpoolAddress)).isTrue();

        // Logs
        assertThat(iexecHubService.fetchLogTopics(matchOrdersTxHash))
                .isEqualTo(List.of("Transfer", "Lock", "Transfer", "Lock", "SchedulerNotice", "OrdersMatched"));
        assertThat(iexecHubService.fetchLogTopics(initializeTxHash)).isEqualTo(List.of("TaskInitialize"));
        assertThat(iexecHubService.fetchLogTopics(contributeAndFinalizeTxHash))
                .isEqualTo(List.of("Seize", "Transfer", "Unlock", "Transfer", "Reward", "Transfer", "Reward", "TaskContribute", "TaskConsensus", "TaskReveal", "TaskFinalize"));

        web3jService.showReceipt(matchOrdersTxHash, "matchOrders");
        web3jService.showReceipt(initializeTxHash, "initialize");
        web3jService.showReceipt(contributeAndFinalizeTxHash, "contributeAndFinalize");
    }

}
