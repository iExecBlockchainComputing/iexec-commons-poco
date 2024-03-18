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

import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.encoding.MatchOrdersDataEncoder;
import com.iexec.commons.poco.order.AppOrder;
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.order.RequestOrder;
import com.iexec.commons.poco.order.WorkerpoolOrder;
import com.iexec.commons.poco.utils.BytesUtils;
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
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static com.iexec.commons.poco.itest.IexecHubTestService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.BLOCK_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@Tag("itest")
@Testcontainers
class MatchOrdersTests {

    private Credentials credentials;
    private SignerService signerService;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;
    private OrdersService ordersService;

    @Container
    static ComposeContainer environment = new ComposeContainer(new File("docker-compose.yml"))
            .withExposedService("poco-chain", 8545);

    @BeforeEach
    void init() throws CipherException, IOException {
        credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        String chainNodeAddress = "http://" + environment.getServiceHost(SERVICE_NAME, SERVICE_PORT) + ":" +
                environment.getServicePort(SERVICE_NAME, SERVICE_PORT);
        web3jService = new Web3jTestService(chainNodeAddress);
        iexecHubService = new IexecHubTestService(credentials, web3jService);
        signerService = new SignerService(web3jService.getWeb3j(), web3jService.getChainId(), credentials);
        ordersService = new OrdersService(signerService);
    }

    @Test
    void shouldBeConnectedToNode() {
        assertThat(web3jService.isConnected()).isTrue();
        assertDoesNotThrow(() -> web3jService.checkConnection());
    }

    @Test
    void shouldMatchOrdersWithIexecHubService() throws Exception {
        String appAddress = iexecHubService.createApp(
                "my-app-1",
                "multiAddress",
                "DOCKER",
                BytesUtils.EMPTY_HEX_STRING_32,
                "{}"
        );
        assertThat(appAddress).isEqualTo("0x564ef252a271ff68a74266e8f574ed827297caf4");

        String datasetAddress = iexecHubService.createDataset(
                "my-dataset-1",
                "multiAddress",
                BytesUtils.EMPTY_HEX_STRING_32
        );
        assertThat(datasetAddress).isEqualTo("0x8e0c3441b788fe2bafb29a729c59d9e9a9ca5483");

        String workerpoolAddress = iexecHubService.createWorkerpool("my-workerpool-1");
        assertThat(workerpoolAddress).isEqualTo("0x1d9340e5c33c6d289250df3b1ce31bc3ac6b16c0");

        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(appAddress);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0xbcb64941a9e7162f6f3a2b9e636596eec1bee8d420556469e11641905b45c7785691c310e4cce295311ebf6439acf2a6a943fb8d5db87a1971f979a83e6ea34d1b");

        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(datasetAddress);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0x014011344b5684761605fd34ecdcebd34b1efc249658a1f975d8c2a26a7adc5b68dcda7bd298cc578aea99877b5ae4e7b0f3cacd307cd4fdfb382d87a0fdc5e11c");

        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(workerpoolAddress);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0xb3b4cc51859bf106562baecf6261bfa8e168a11e06dcc82b91d0ccfbe16be14c414526c4a2e0908ef61eae6a91ec96a8d977fda4b204ac06d1b7e477abe8ea8d1b");

        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0xa6a6eea2c49c7df388d8a265926f19bf4cac049f73875f1dbbe813ce088a7e833a2552cb33a14927b350c858349d3bcedbe23286edf912ad585dc102a1249e751c");

        BigInteger nonce = web3jService.getNonce(credentials.getAddress());
        String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        String predictedDealId = signerService.sendCall(IEXEC_HUB_ADDRESS, matchOrdersTxData);
        String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData
        );
        await().atMost(BLOCK_TIME, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(matchOrdersTxHash));

        final TransactionReceipt receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        assertThat(receipt).isNotNull();
        assertThat(receipt.isStatusOK()).isTrue();

        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);
        byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;

        String chainDealId = BytesUtils.bytesToString(dealid);
        assertThat(iexecHubService.getChainDeal(chainDealId)).isPresent();
        assertThat(chainDealId).isEqualTo(predictedDealId);
    }

    @Test
    void shouldMatchOrdersWithSignerService() throws Exception {
        final String predictedAppAddress = iexecHubService.callCreateApp("my-app-2");
        final String predictedDatasetAddress = iexecHubService.callCreateDataset("my-dataset-2");
        final String predictedWorkerpoolAddress = iexecHubService.callCreateWorkerpool("my-workerpool-2");
        BigInteger nonce = web3jService.getNonce(credentials.getAddress());
        final String appTxHash = iexecHubService.submitCreateAppTx(nonce, "my-app-2");
        nonce = nonce.add(BigInteger.ONE);
        final String datasetTxHash = iexecHubService.submitCreateDatasetTx(nonce, "my-dataset-2");
        nonce = nonce.add(BigInteger.ONE);
        final String workerpoolTxHash = iexecHubService.submitCreateWorkerpoolTx(nonce, "my-workerpool-2");

        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(predictedAppAddress);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0xc200bf4ac4170d3949831d0049a184b7ee612d74a54f4d12af62cfa734330b3523c1db3a57a4f91d03932313aed3c95200e7950d52cc2f98ee0e08cb0648cbf31c");

        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(predictedDatasetAddress);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0xfa2bf8bbfde7db118970aae8781dc78210bd9433111f1332f549aa003f7f28cc6c68c038aa35b5ddfc98242a8ede07ea8fa073f2b0e7f1247b8b97fadc4837c91c");

        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(predictedWorkerpoolAddress);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0xf6474336a10e3ec12ff65e721fee6ba53763f937a1b54659332f61cf1770b4b26947df22ad89e8623aa985763e917e9f66670e51befc7195b16b0e8db365c13c1c");

        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0x17d53276b2f125953d147af94381388ee20e6be80a6c76cf5359c8a1f04c0f0b670a3bd890fdca9307a11ef52927634b65399fa3a03fc6e89f17d5e3c383b4c31c");

        nonce = nonce.add(BigInteger.ONE);
        final String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        final String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData
        );
        await().atMost(BLOCK_TIME, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(appTxHash, datasetTxHash, workerpoolTxHash, matchOrdersTxHash));
        assertThat(web3jService.areTxStatusOK(appTxHash, datasetTxHash, workerpoolTxHash, matchOrdersTxHash)).isTrue();

        final TransactionReceipt receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);

        byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;
        String chainDealId = BytesUtils.bytesToString(dealid);
        assertThat(iexecHubService.getChainDeal(chainDealId)).isPresent();

        assertThat(web3jService.getDeployedAssets(appTxHash, datasetTxHash, workerpoolTxHash))
                .containsExactly(predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress);
    }

}
