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
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static com.iexec.commons.poco.itest.IexecHubTestService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.BLOCK_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@Tag("itest")
@Testcontainers
class MatchOrdersTests {

    private SignerService signerService;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;
    private OrdersService ordersService;

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
    void shouldBeConnectedToNode() {
        assertThat(web3jService.isConnected()).isTrue();
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
        assertThat(appAddress).isEqualTo("0x909375d8bc4e9c26afa996bad7571b88b6cefa72");

        String datasetAddress = iexecHubService.createDataset(
                "my-dataset-1",
                "multiAddress",
                BytesUtils.EMPTY_HEX_STRING_32
        );
        assertThat(datasetAddress).isEqualTo("0x049d8b40e50282ae8b34780240ff49235b87bf59");

        String workerpoolAddress = iexecHubService.createWorkerpool("my-workerpool-1");
        assertThat(workerpoolAddress).isEqualTo("0x4f1e8ec6d1fb6ce8309d816ecacde77da4f7de9c");

        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(appAddress);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0x95bc07e7a64415d7529ded8c4730c17870d519e569857533e273f350235b4ebf17d46ffff80b37452cca1b6540db127cd991ce91c6cb7ba3d2a1bc9409d70ad51b");

        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(datasetAddress);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0x08195ca0a92aea60ff6e08c7b8d089d50b650a4ac214d1233d6857cc58349a8f710fdc16121bf028e035b26774c2a341c67e556119bec42532c4e0586256f5481b");

        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(workerpoolAddress);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0x012dd3697776b6ce1a8ea8dc8541155dff59422bc9f21c28e61009817934ec943ff274d3831432b9ee6b2206b8b0a7e669e38cf8fa9c73bd339dd6a6e1514c8b1b");

        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0x66d7c636055ff2d03e5250065cc24481ed7bfa80cc0b92f2437feb9690c9ad30077351549030ab65eaf647de8b2b9e7ac758d6b6cb53550d4771b213b5f206e11b");

        BigInteger nonce = signerService.getNonce();
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
        assertThat(iexecHubService.getChainDealWithDetails(chainDealId)).isPresent();
        assertThat(chainDealId).isEqualTo(predictedDealId);
    }

    @Test
    void shouldMatchOrdersWithSignerService() throws IOException {
        final String appName = "my-app-2";
        final String datasetName = "my-dataset-2";
        final String workerpoolName = "my-workerpool-2";
        final String predictedAppAddress = iexecHubService.callCreateApp(appName);
        final String predictedDatasetAddress = iexecHubService.callCreateDataset(datasetName);
        final String predictedWorkerpoolAddress = iexecHubService.callCreateWorkerpool(workerpoolName);
        BigInteger nonce = signerService.getNonce();
        final String appTxHash = iexecHubService.submitCreateAppTx(nonce, appName);
        nonce = nonce.add(BigInteger.ONE);
        final String datasetTxHash = iexecHubService.submitCreateDatasetTx(nonce, datasetName);
        nonce = nonce.add(BigInteger.ONE);
        final String workerpoolTxHash = iexecHubService.submitCreateWorkerpoolTx(nonce, workerpoolName);

        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(predictedAppAddress);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0x8a3d3283f11e22318ed65fc22dcf1101a5905c1fbfb2dee67981668de7130647175ca7b728f066d764006b9e95cc4f8097157de7ee6b6d917479f6598d055ff81b");

        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(predictedDatasetAddress);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0x936734f59674cb89ba77616b4a00f91b9b5ec938723b4465b15bca74e3e8274a72db4ad60cc5097dbecd5f721f7d241efe5905fe6d49968dffe6a14ca33c82951b");

        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(predictedWorkerpoolAddress);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0x0d204f203e07e8250bb587fbcd64adf8da95accf2f3cc686e9585794f3d1aad076a996d5d2fe6ff4b4cabd8f127fc7543468d784c74420a8440d9742a72bd0ee1b");

        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0xf6117dcc9a2feac58fcf3bb9b4cfebac9cd2f538c6611dce8ff2c3a7ea1d5c464c4e46e8622f5faeb2ec88dc6e5231fb97163494acbccf54bf8650477b11c4781b");

        nonce = nonce.add(BigInteger.ONE);
        final String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        final String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData);

        await().atMost(BLOCK_TIME, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(appTxHash, datasetTxHash, workerpoolTxHash, matchOrdersTxHash));
        assertThat(web3jService.areTxStatusOK(appTxHash, datasetTxHash, workerpoolTxHash, matchOrdersTxHash)).isTrue();

        final TransactionReceipt receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);

        final byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;
        final String chainDealId = BytesUtils.bytesToString(dealid);
        assertThat(iexecHubService.getChainDeal(chainDealId)).isPresent();
        assertThat(iexecHubService.getChainDealWithDetails(chainDealId)).isPresent();

        assertThat(web3jService.getDeployedAssets(appTxHash, datasetTxHash, workerpoolTxHash))
                .containsExactly(predictedAppAddress, predictedDatasetAddress, predictedWorkerpoolAddress);

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
        assertThat(iexecHubService.fetchLogTopics(appTxHash)).isEqualTo(List.of("Transfer"));
        assertThat(iexecHubService.fetchLogTopics(datasetTxHash)).isEqualTo(List.of("Transfer"));
        assertThat(iexecHubService.fetchLogTopics(workerpoolTxHash)).isEqualTo(List.of("Transfer"));
        assertThat(iexecHubService.fetchLogTopics(matchOrdersTxHash))
                .isEqualTo(List.of("Transfer", "Lock", "Transfer", "Lock", "SchedulerNotice", "OrdersMatched"));
    }

}
