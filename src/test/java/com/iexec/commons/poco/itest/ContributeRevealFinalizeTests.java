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
import lombok.SneakyThrows;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static com.iexec.commons.poco.itest.IexecHubTestService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.MINING_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@Tag("itest")
@Testcontainers
class ContributeRevealFinalizeTests {

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
        final String chainNodeAddress = "http://" + environment.getServiceHost(SERVICE_NAME, SERVICE_PORT) + ":" +
                environment.getServicePort(SERVICE_NAME, SERVICE_PORT);
        web3jService = new Web3jTestService(chainNodeAddress);
        iexecHubService = new IexecHubTestService(credentials, web3jService);
        signerService = new SignerService(web3jService.getWeb3j(), web3jService.getChainId(), credentials);
        ordersService = new OrdersService(iexecHubService.getOrdersDomain(), signerService);
    }

    @SneakyThrows
    private String workerContribute(final SignerService workerSigner, final String chainTaskId) {
        final SignerService enclaveSigner = new SignerService(web3jService.getWeb3j(), web3jService.getChainId());
        final String enclaveChallenge = enclaveSigner.getAddress();

        final String resultHash = HashUtils.concatenateAndHash(chainTaskId, TeeUtils.TEE_SCONE_ONLY_TAG);
        final String resultSeal = HashUtils.concatenateAndHash(workerSigner.getAddress(), chainTaskId, TeeUtils.TEE_SCONE_ONLY_TAG);
        final String messageHash = HashUtils.concatenateAndHash(resultHash, resultSeal);
        final String enclaveSignature = enclaveSigner.signMessageHash(messageHash).getValue();

        final String authorizationHash = HashUtils.concatenateAndHash(workerSigner.getAddress(), chainTaskId, enclaveChallenge);
        final String wpAuthorizationSignature = signerService.signMessageHash(authorizationHash).getValue();

        final String contributeTxData = PoCoDataEncoder.encodeContribute(chainTaskId, resultHash, resultSeal, enclaveChallenge, enclaveSignature, wpAuthorizationSignature);
        return workerSigner.signAndSendTransaction(
                BigInteger.ZERO, BigInteger.ZERO, GAS_LIMIT, IEXEC_HUB_ADDRESS, contributeTxData);
    }

    @SneakyThrows
    private String workerReveal(final SignerService workerSigner, final String chainTaskId) {
        final String revealTxData = PoCoDataEncoder.encodeReveal(chainTaskId, TeeUtils.TEE_SCONE_ONLY_TAG);
        return workerSigner.signAndSendTransaction(
                BigInteger.ONE, BigInteger.ZERO, GAS_LIMIT, IEXEC_HUB_ADDRESS, revealTxData);
    }

    @Test
    void shouldContributeRevealFinalize() throws Exception {
        final Map<String, String> assetAddresses = iexecHubService.deployAssets();
        final String predictedAppAddress = assetAddresses.get("app");
        final String predictedDatasetAddress = assetAddresses.get("dataset");
        final String predictedWorkerpoolAddress = assetAddresses.get("workerpool");

        final OrdersService.DealOrders dealOrders = ordersService.buildAllSignedOrders(
                predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress, BigInteger.ONE);
        final String predictedDealId = ordersService.callMatchOrders(dealOrders);
        final String predictedChainTaskId = ChainUtils.generateChainTaskId(predictedDealId, 0);

        // deal id
        BigInteger nonce = signerService.getNonce();
        final String matchOrdersTxHash = ordersService.submitMatchOrders(dealOrders, nonce);

        // init
        final String initializeTxData = PoCoDataEncoder.encodeInitialize(predictedDealId, 0);
        nonce = nonce.add(BigInteger.ONE);
        final String initializeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, initializeTxData);

        // worker contribute and reveal
        final SignerService workerSigner = web3jService.createSigner();
        final String contributeTxHash = workerContribute(workerSigner, predictedChainTaskId);
        final String revealTxHash = workerReveal(workerSigner, predictedChainTaskId);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(matchOrdersTxHash, initializeTxHash, contributeTxHash, revealTxHash));

        // finalize needs to be separated to avoid executing the transaction before contribute and reveal
        final String finalizeTxData = PoCoDataEncoder.encodeFinalize(predictedChainTaskId, new byte[0], new byte[0]);
        nonce = nonce.add(BigInteger.ONE);
        final String finalizeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, finalizeTxData);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(finalizeTxHash));

        assertThat(web3jService.areTxStatusOK(matchOrdersTxHash, initializeTxHash, contributeTxHash, revealTxHash, finalizeTxHash)).isTrue();

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
        assertThat(iexecHubService.fetchLogTopics(contributeTxHash)).isEqualTo(List.of("Transfer", "Lock", "TaskContribute", "TaskConsensus"));
        assertThat(iexecHubService.fetchLogTopics(revealTxHash)).isEqualTo(List.of("TaskReveal"));
        assertThat(iexecHubService.fetchLogTopics(finalizeTxHash))
                .isEqualTo(List.of("Seize", "Transfer", "Unlock", "Transfer", "Unlock", "Transfer", "Reward", "Transfer", "Reward", "TaskFinalize"));

        web3jService.showReceipt(matchOrdersTxHash, "matchOrders");
        web3jService.showReceipt(initializeTxHash, "initialize");
        web3jService.showReceipt(contributeTxHash, "contribute");
        web3jService.showReceipt(revealTxHash, "reveal");
        web3jService.showReceipt(finalizeTxHash, "finalize");
    }

    @Test
    void shouldContributeRevealFinalizeWith2contributions() throws Exception {
        final Map<String, String> assetAddresses = iexecHubService.deployAssets();
        final String predictedAppAddress = assetAddresses.get("app");
        final String predictedDatasetAddress = assetAddresses.get("dataset");
        final String predictedWorkerpoolAddress = assetAddresses.get("workerpool");

        final List<String> txHashes = new ArrayList<>();
        final BigInteger trust = BigInteger.valueOf(3);

        BigInteger nonce = signerService.getNonce();

        final OrdersService.DealOrders dealOrders = ordersService.buildAllSignedOrders(
                predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress, trust);
        final String predictedDealId = ordersService.callMatchOrders(dealOrders);
        final String predictedChainTaskId = ChainUtils.generateChainTaskId(predictedDealId, 0);

        // deal id
        txHashes.add(ordersService.submitMatchOrders(dealOrders, nonce));

        // init
        final String initializeTxData = PoCoDataEncoder.encodeInitialize(predictedDealId, 0);
        nonce = signerService.getNonce();
        txHashes.add(signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, initializeTxData));

        final SignerService worker1 = web3jService.createSigner();
        final SignerService worker2 = web3jService.createSigner();

        // contribute
        final List<String> contributeTxHashes = Stream.of(worker1, worker2)
                .map(worker -> workerContribute(worker, predictedChainTaskId))
                .toList();
        txHashes.addAll(contributeTxHashes);

        // reveal
        final List<String> revealTxHashes = Stream.of(worker1, worker2)
                .map(worker -> workerReveal(worker, predictedChainTaskId))
                .toList();
        txHashes.addAll(revealTxHashes);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(txHashes.toArray(String[]::new)));

        // finalize
        final String finalizeTxData = PoCoDataEncoder.encodeFinalize(predictedChainTaskId, new byte[0], new byte[0]);
        nonce = signerService.getNonce();
        final String finalizeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, finalizeTxData);
        txHashes.add(finalizeTxHash);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(finalizeTxHash));

        assertThat(web3jService.areTxStatusOK(txHashes.toArray(String[]::new))).isTrue();

        for (final String txHash : txHashes) {
            web3jService.showReceipt(txHash, "");
        }
    }

}
