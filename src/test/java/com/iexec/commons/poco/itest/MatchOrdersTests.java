/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.ChainDeal;
import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.eip712.OrderSigner;
import com.iexec.commons.poco.order.*;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@Tag("itest")
@Testcontainers
class MatchOrdersTests {

    private static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";

    private Credentials credentials;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;
    private OrderSigner signer;

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(new File("docker-compose.yml"))
            .withExposedService("poco-chain", 8545);

    @BeforeEach
    void init() throws CipherException, IOException {
        credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        web3jService = new Web3jTestService(environment.getServicePort("poco-chain", 8545));
        iexecHubService = new IexecHubTestService(credentials, web3jService);
        signer = new OrderSigner(65535, IEXEC_HUB_ADDRESS, credentials.getEcKeyPair());
    }

    @Test
    void shouldBeConnectedToNode() {
        assertThat(web3jService.isConnected()).isTrue();
        assertDoesNotThrow(() -> web3jService.checkConnection());
    }

    @Test
    void shouldMatchOrder() throws Exception {
        String appAddress = iexecHubService.createApp(
                "my-app",
                "multiAddress",
                "DOCKER",
                BytesUtils.EMPTY_HEX_STRING_32,
                "{}"
        );
        assertThat(appAddress).isEqualTo("0x0677c9ad40e1c3508b40bfb1c4749cc9bd63933f");

        String datasetAddress = iexecHubService.createDataset(
                "my-dataset",
                "multiAddress",
                BytesUtils.EMPTY_HEX_STRING_32
        );
        assertThat(datasetAddress).isEqualTo("0xe203f571c8d7d2abcf5e406d20965e3889662f5e");

        String workerpoolAddress = iexecHubService.createWorkerpool("my-workerpool");
        assertThat(workerpoolAddress).isEqualTo("0x74c6683f7bc258946e01e278b2842c99a0c7896a");

        AppOrder appOrder = AppOrder.builder()
                .app(appAddress)
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String("abcd"))
                .build();

        AppOrder signedAppOrder = signer.signAppOrder(appOrder);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0x18e0f7a382513a74e90763dc755c0751121316073b0f4cb6a5481580696574ec3e0060c166bc1b764079d233236ff59e88bcb74bbe7c941d2cdf7204f5fc89061b");

        DatasetOrder datasetOrder = DatasetOrder.builder()
                .dataset(datasetAddress)
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String("abcd"))
                .build();

        DatasetOrder signedDatasetOrder = signer.signDatasetOrder(datasetOrder);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0x529ea0d91a7f1cc373c34c7ec43cd132238b052abbf74c379f15d930fe0bf66d00907b58ba2bbe9e1f73aeb02e08da6a4e6915dba7ed4eb54a56b2d319ec987b1b");

        WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
                .workerpool(workerpoolAddress)
                .workerpoolprice(BigInteger.TEN)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String("abcd"))
                .build();
        WorkerpoolOrder signedWorkerpoolOrder = signer.signWorkerpoolOrder(workerpoolOrder);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0x52ce5afec8e142ea9217bd818796c7b4b1ab1e1ebebf532b59ebc3ae15c56efb5c55e1b3c79fa3929ae1bf3895b926f47316dac8a7a484c8db05068d31e8038f1c");

        TreeMap<String, String> iexecSecrets = new TreeMap<>(Map.of(
                "1", "first-secret",
                "2", "second-secret",
                "3", "third-secret"));
        DealParams dealParams = DealParams.builder()
                .iexecResultEncryption(true)
                .iexecResultStorageProvider("ipfs")
                .iexecResultStorageProxy("http://result-proxy:13200")
                .iexecSecrets(iexecSecrets)
                .build();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(appOrder.getAppprice())
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(datasetOrder.getDatasetprice())
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(workerpoolOrder.getWorkerpoolprice())
                .requester(credentials.getAddress())
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .beneficiary(credentials.getAddress())
                .callback(BytesUtils.EMPTY_ADDRESS)
                .params(dealParams.toJsonString())
                .salt(Hash.sha3String("abcd"))
                .build();
        RequestOrder signedRequestOrder = signer.signRequestOrder(requestOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0x4738fe5e4b7fc501a82a1de2a5faaf64608049e87a15508d72dcee16e61c543a25b87c4ed6f47a16a3befea950a90a8e6ace7f3029a4c152765c0e67a1cb2e651b");

        TransactionReceipt receipt = iexecHubService
                .getHubContract()
                .matchOrders(
                        signedAppOrder.toHubContract(),
                        signedDatasetOrder.toHubContract(),
                        signedWorkerpoolOrder.toHubContract(),
                        signedRequestOrder.toHubContract()
                ).send();

        assertThat(receipt).isNotNull();
        assertThat(receipt.isStatusOK()).isTrue();

        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);
        byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;

        String chainDealId = BytesUtils.bytesToString(dealid);
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainDealId);
        assertThat(oChainDeal).isPresent();
    }

}
