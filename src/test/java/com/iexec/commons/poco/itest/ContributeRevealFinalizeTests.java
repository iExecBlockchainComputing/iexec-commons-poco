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
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.IexecHubTestService.*;
import static com.iexec.commons.poco.itest.OrdersService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.BLOCK_TIME;
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
            .withExposedService("poco-chain", 8545);

    @BeforeEach
    void init() throws CipherException, IOException {
        final Credentials credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        final String chainNodeAddress = "http://" + environment.getServiceHost("poco-chain", 8545) + ":" +
                environment.getServicePort("poco-chain", 8545);
        web3jService = new Web3jTestService(chainNodeAddress);
        iexecHubService = new IexecHubTestService(credentials, web3jService);
        signerService = new SignerService(web3jService.getWeb3j(), web3jService.getChainId(), credentials);
        ordersService = new OrdersService(signerService);
    }

    @Test
    void shouldContributeRevealFinalize() throws Exception {
        final String predictedAppAddress = iexecHubService.callCreateApp(APP_NAME);
        final String predictedDatasetAddress = iexecHubService.callCreateDataset(DATASET_NAME);
        final String predictedWorkerpoolAddress = iexecHubService.callCreateWorkerpool(WORKERPOOL_NAME);
        final BigInteger estimatedCreateAppGas = iexecHubService.estimateCreateApp(APP_NAME);
        final BigInteger estimatedCreateDatasetGas = iexecHubService.estimateCreateDataset(DATASET_NAME);
        final BigInteger estimatedCreateWorkerpoolGas = iexecHubService.estimateCreateWorkerpool(WORKERPOOL_NAME);
        BigInteger nonce = web3jService.getNonce(signerService.getAddress());
        final String appTxHash = iexecHubService.submitCreateAppTx(nonce, estimatedCreateAppGas, APP_NAME);
        nonce = nonce.add(BigInteger.ONE);
        final String datasetTxHash = iexecHubService.submitCreateDatasetTx(nonce, estimatedCreateDatasetGas, DATASET_NAME);
        nonce = nonce.add(BigInteger.ONE);
        final String workerpoolTxHash = iexecHubService.submitCreateWorkerpoolTx(nonce, estimatedCreateWorkerpoolGas, WORKERPOOL_NAME);

        // Wait for assets deployment to be able to call MatchOrders
        await().atMost(BLOCK_TIME, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(appTxHash, datasetTxHash, workerpoolTxHash));

        final String predictedDealId = ordersService.callMatchOrders(
                predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress);
        final String predictedChainTaskId = ChainUtils.generateChainTaskId(predictedDealId, 0);
        final BigInteger estimatedMatchOrdersGas = ordersService.estimateMatchOrders(
                predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress);

        // deal id
        nonce = nonce.add(BigInteger.ONE);
        final String matchOrdersTxHash = ordersService.submitMatchOrders(
                predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress, nonce);

        // init
        final String initializeTxData = PoCoDataEncoder.encodeInitialize(predictedDealId, 0);
        nonce = web3jService.getNonce(signerService.getAddress());
        final String initializeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, initializeTxData);

        // enclave challenge
        final SignerService enclaveSigner = new SignerService(web3jService.getWeb3j(), web3jService.getChainId());
        final String enclaveChallenge = enclaveSigner.getAddress();

        // contribute
        final String resultDigest = TeeUtils.TEE_SCONE_ONLY_TAG;
        final String resultHash = HashUtils.concatenateAndHash(predictedChainTaskId, resultDigest);
        final String resultSeal = HashUtils.concatenateAndHash(signerService.getAddress(), predictedChainTaskId, resultDigest);

        final String messageHash = HashUtils.concatenateAndHash(resultHash, resultSeal);
        final String enclaveSignature = enclaveSigner.signMessageHash(messageHash).getValue();
        final String authorizationHash = HashUtils.concatenateAndHash(signerService.getAddress(), predictedChainTaskId, enclaveChallenge);
        final String wpAuthorizationSignature = signerService.signMessageHash(authorizationHash).getValue();

        final String contributeData = PoCoDataEncoder.encodeContribute(predictedChainTaskId, resultHash, resultSeal, enclaveChallenge, enclaveSignature, wpAuthorizationSignature);
        nonce = web3jService.getNonce(signerService.getAddress());
        final String contributeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, contributeData);

        // reveal
        final String revealTxData = PoCoDataEncoder.encodeReveal(predictedChainTaskId, resultDigest);
        nonce = web3jService.getNonce(signerService.getAddress());
        final String revealTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, revealTxData);

        // finalize
        final String finalizeTxData = PoCoDataEncoder.encodeFinalize(predictedChainTaskId, new byte[0], new byte[0]);
        nonce = web3jService.getNonce(signerService.getAddress());
        final String finalizeTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, finalizeTxData);

        await().atMost(BLOCK_TIME, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(matchOrdersTxHash, contributeTxHash, revealTxHash, finalizeTxHash));
        assertThat(web3jService.areTxStatusOK(matchOrdersTxHash, initializeTxHash, contributeTxHash, revealTxHash, finalizeTxHash)).isTrue();

        // checks
        assertThat(web3jService.getDeployedAssets(appTxHash, datasetTxHash, workerpoolTxHash))
                .containsExactly(predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress);
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

        // Gas
        web3jService.displayGas("createApp", estimatedCreateAppGas, appTxHash);
        web3jService.displayGas("createDataset", estimatedCreateDatasetGas, datasetTxHash);
        web3jService.displayGas("createWorkerpool", estimatedCreateWorkerpoolGas, workerpoolTxHash);
        web3jService.displayGas("matchOrders", estimatedMatchOrdersGas, matchOrdersTxHash);
    }

}
