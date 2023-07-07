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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@Tag("itest")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchOrdersTests {

    private static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";

    private Credentials credentials;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;
    private OrderSigner signer;

    private String appAddress;
    private String datasetAddress;
    private String workerpoolAddress;

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(new File("docker-compose.yml"))
            .withExposedService("poco-chain", 8545);

    @BeforeEach
    void init(TestInfo testInfo) throws CipherException, IOException {
        log.info(">>> {}", testInfo.getDisplayName());
        log.info("init {} {} {}", appAddress, datasetAddress, workerpoolAddress);
        credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        web3jService = new Web3jTestService(environment.getServicePort("poco-chain", 8545));
        iexecHubService = new IexecHubTestService(credentials, web3jService);
        signer = new OrderSigner(65535, IEXEC_HUB_ADDRESS, credentials.getEcKeyPair());
        deployAssets();
    }

    void deployAssets() {
        log.info("deployAssets {} {} {}", appAddress, datasetAddress, workerpoolAddress);
        if (appAddress == null) {
            log.info("deploying app");
            appAddress = iexecHubService.createApp(
                    "my-app",
                    "multiAddress",
                    "DOCKER",
                    BytesUtils.EMPTY_HEX_STRING_32,
                    "{}"
            );
            log.info("app deployed at {}", appAddress);
        }

        if (datasetAddress == null) {
            log.info("deploying dataset");
            datasetAddress = iexecHubService.createDataset(
                    "my-dataset",
                    "multiAddress",
                    BytesUtils.EMPTY_HEX_STRING_32
            );
            log.info("dataset deployed at {}", datasetAddress);
        }

        if (workerpoolAddress == null) {
            log.info("deploying workerpool");
            workerpoolAddress = iexecHubService.createWorkerpool("my-workerpool");
            log.info("workerpool deployed at {}", workerpoolAddress);
        }
        log.info("deployAssets {} {} {}", appAddress, datasetAddress, workerpoolAddress);
    }

    @Test
    void shouldBeConnectedToNode() {
        assertThat(web3jService.isConnected()).isTrue();
        assertDoesNotThrow(() -> web3jService.checkConnection());
    }

    @ParameterizedTest
    @MethodSource("provideValidTagsCombinations")
    void shouldMatchOrders(
            long workerpoolOrderTag,
            long requestOrderTag,
            long appOrderTag,
            long datasetOrderTag) throws Exception {
        AppOrder appOrder = createAppOrder(appOrderTag);
        AppOrder signedAppOrder = signer.signAppOrder(appOrder);

        DatasetOrder datasetOrder = createDatasetOrder(datasetOrderTag);
        DatasetOrder signedDatasetOrder = signer.signDatasetOrder(datasetOrder);

        WorkerpoolOrder workerpoolOrder = createWorkerpoolOrder(workerpoolOrderTag);
        WorkerpoolOrder signedWorkerpoolOrder = signer.signWorkerpoolOrder(workerpoolOrder);

        RequestOrder requestOrder = createRequestOrder(
                requestOrderTag, appOrder, datasetOrder, workerpoolOrder);
        RequestOrder signedRequestOrder = signer.signRequestOrder(requestOrder);

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

    private Stream<Arguments> provideValidTagsCombinations() {
        return Stream.of(
                Arguments.of(0x0, 0x0, 0x0, 0x0),
                Arguments.of(0x3, 0x3, 0x3, 0x3),
                Arguments.of(0x5, 0x5, 0x5, 0x5),
                Arguments.of(0x3, 0x3, 0x3, 0x0),
                Arguments.of(0x3, 0x0, 0x3, 0x0),
                Arguments.of(0x3, 0x3, 0x3, 0x1),
                Arguments.of(0x5, 0x5, 0x1, 0x0),
                Arguments.of(0x7, 0x3, 0x5, 0x0)  // Match Scone request with Gramine app order and standard dataset
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTagsCombinations")
    void shouldNotMatchOrders(
            long workerpoolOrderTag,
            long requestOrderTag,
            long appOrderTag,
            long datasetOrderTag) {

        AppOrder appOrder = createAppOrder(appOrderTag);
        AppOrder signedAppOrder = signer.signAppOrder(appOrder);

        DatasetOrder datasetOrder = createDatasetOrder(datasetOrderTag);
        DatasetOrder signedDatasetOrder = signer.signDatasetOrder(datasetOrder);

        WorkerpoolOrder workerpoolOrder = createWorkerpoolOrder(workerpoolOrderTag);
        WorkerpoolOrder signedWorkerpoolOrder = signer.signWorkerpoolOrder(workerpoolOrder);

        RequestOrder requestOrder = createRequestOrder(
                requestOrderTag, appOrder, datasetOrder, workerpoolOrder);
        RequestOrder signedRequestOrder = signer.signRequestOrder(requestOrder);

        assertThrows(TransactionException.class, () -> iexecHubService
                .getHubContract()
                .matchOrders(
                        signedAppOrder.toHubContract(),
                        signedDatasetOrder.toHubContract(),
                        signedWorkerpoolOrder.toHubContract(),
                        signedRequestOrder.toHubContract()
                ).send());
    }

    private Stream<Arguments> provideInvalidTagsCombinations() {
        return Stream.of(
                Arguments.of(0x0, 0x1, 0x1, 0x1),
                Arguments.of(0x0, 0x1, 0x1, 0x1),
                Arguments.of(0x0, 0x3, 0x3, 0x3),
                Arguments.of(0x0, 0x5, 0x5, 0x5),
                Arguments.of(0x1, 0x2, 0x1, 0x1), // Workerpool order does not match 0x3
                Arguments.of(0x3, 0x3, 0x2, 0x3), // Missing TEE bit on app order tag
                Arguments.of(0x5, 0x2, 0x5, 0x5), // Workerpool order does not match 0x7
                Arguments.of(0x5, 0x3, 0x0, 0x0),
                Arguments.of(0x3, 0x3, 0x0, 0x0)
        );
    }

    AppOrder createAppOrder(long tag) {
        return AppOrder.builder()
                .app(appAddress)
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(BytesUtils.toByte32HexString(tag))
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    DatasetOrder createDatasetOrder(long tag) {
        return DatasetOrder.builder()
                .dataset(datasetAddress)
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(BytesUtils.toByte32HexString(tag))
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    WorkerpoolOrder createWorkerpoolOrder(long tag) {
        return WorkerpoolOrder.builder()
                .workerpool(workerpoolAddress)
                .workerpoolprice(BigInteger.TEN)
                .volume(BigInteger.ONE)
                .tag(BytesUtils.toByte32HexString(tag))
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    RequestOrder createRequestOrder(
            long tag,
            AppOrder appOrder,
            DatasetOrder datasetOrder,
            WorkerpoolOrder workerpoolOrder) {
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
        return RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(appOrder.getAppprice())
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(datasetOrder.getDatasetprice())
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(workerpoolOrder.getWorkerpoolprice())
                .requester(credentials.getAddress())
                .volume(BigInteger.ONE)
                .tag(BytesUtils.toByte32HexString(tag))
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .beneficiary(credentials.getAddress())
                .callback(BytesUtils.EMPTY_ADDRESS)
                .params(dealParams.toJsonString())
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

}
