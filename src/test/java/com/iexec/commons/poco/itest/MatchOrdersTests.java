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

import com.iexec.commons.poco.chain.ChainDeal;
import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.eip712.OrderSigner;
import com.iexec.commons.poco.encoding.AssetDataEncoder;
import com.iexec.commons.poco.encoding.MatchOrdersDataEncoder;
import com.iexec.commons.poco.order.*;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@Tag("itest")
@Testcontainers
class MatchOrdersTests {

    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_000_000L);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
    private static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";

    private Credentials credentials;
    private SignerService signerService;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;
    private OrderSigner signer;

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
        signer = new OrderSigner(65535, IEXEC_HUB_ADDRESS, credentials.getEcKeyPair());
        signerService = new SignerService(web3jService.getWeb3j(), web3jService.getChainId(), credentials);
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

        final AppOrder signedAppOrder = buildSignedAppOrder(appAddress);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0xbcb64941a9e7162f6f3a2b9e636596eec1bee8d420556469e11641905b45c7785691c310e4cce295311ebf6439acf2a6a943fb8d5db87a1971f979a83e6ea34d1b");

        final DatasetOrder signedDatasetOrder = buildSignedDatasetOrder(datasetAddress);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0x014011344b5684761605fd34ecdcebd34b1efc249658a1f975d8c2a26a7adc5b68dcda7bd298cc578aea99877b5ae4e7b0f3cacd307cd4fdfb382d87a0fdc5e11c");

        final WorkerpoolOrder signedWorkerpoolOrder = buildSignedWorkerpoolOrder(workerpoolAddress);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0xb3b4cc51859bf106562baecf6261bfa8e168a11e06dcc82b91d0ccfbe16be14c414526c4a2e0908ef61eae6a91ec96a8d977fda4b204ac06d1b7e477abe8ea8d1b");

        final RequestOrder signedRequestOrder = buildSignedRequestOrder(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0xa6a6eea2c49c7df388d8a265926f19bf4cac049f73875f1dbbe813ce088a7e833a2552cb33a14927b350c858349d3bcedbe23286edf912ad585dc102a1249e751c");

        BigInteger nonce = web3jService.getNonce(credentials.getAddress());
        String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData
        );
        TransactionReceipt receipt = null;
        while (receipt == null) {
            TimeUnit.SECONDS.sleep(1);
            receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        }

        assertThat(receipt).isNotNull();
        assertThat(receipt.isStatusOK()).isTrue();

        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);
        byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;

        String chainDealId = BytesUtils.bytesToString(dealid);
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainDealId);
        assertThat(oChainDeal).isPresent();
    }

    @Test
    void shouldMatchOrdersWithSignerService() throws Exception {
        BigInteger nonce = web3jService.getNonce(credentials.getAddress());
        String appRegistryAddress = iexecHubService.getHubContract().appregistry().send();
        String appTxData = AssetDataEncoder.encodeApp(
                credentials.getAddress(),
                "my-ap-2",
                "DOCKER",
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64),
                "{}"
        );
        String appTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, appRegistryAddress, appTxData
        );
        log.info("app tx hash {}", appTxHash);

        nonce = nonce.add(BigInteger.ONE);
        String datasetRegistryAddress = iexecHubService.getHubContract().datasetregistry().send();
        String datasetTxData = AssetDataEncoder.encodeDataset(
                credentials.getAddress(),
                "my-dataset-2",
                "multiAddress",
                Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 64)
        );
        String datasetTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, datasetRegistryAddress, datasetTxData
        );
        log.info("dataset tx hash {}", datasetTxHash);

        nonce = nonce.add(BigInteger.ONE);
        String workerpoolRegistryAddress = iexecHubService.getHubContract().workerpoolregistry().send();
        String workerpoolTxData = AssetDataEncoder.encodeWorkerpool(
                credentials.getAddress(),
                "my-workerpool-2"
        );
        String workerpoolTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, workerpoolRegistryAddress, workerpoolTxData
        );
        log.info("workerpool tx hash {}", workerpoolTxHash);

        TimeUnit.SECONDS.sleep(5L);
        String appAddress = decodeReceipt(web3jService.getTransactionReceipt(appTxHash));
        String datasetAddress = decodeReceipt(web3jService.getTransactionReceipt(datasetTxHash));
        String workerpoolAddress = decodeReceipt(web3jService.getTransactionReceipt(workerpoolTxHash));

        assertThat(appAddress).isEqualTo("0x41eec3f01f579ac5975b6162566999a6f002b2ef");
        assertThat(datasetAddress).isEqualTo("0xb076cc627fb78ab33bb328f43e316547f6c26edc");
        assertThat(workerpoolAddress).isEqualTo("0x0bec41f6e1424340f252b3687a3686a86134ebe3");

        final AppOrder signedAppOrder = buildSignedAppOrder(appAddress);
        assertThat(signedAppOrder.getSign())
                .isEqualTo("0xa86bb2ea025db10e3ba70b8defcd3009a4afee6b39b95a357a62a7cf07b757db76fb0f6dbf4e409262e7a1a8f1695ecdcaca0b465cebd1c77d4b97a6f102a2201b");

        final DatasetOrder signedDatasetOrder = buildSignedDatasetOrder(datasetAddress);
        assertThat(signedDatasetOrder.getSign())
                .isEqualTo("0xfa2bf8bbfde7db118970aae8781dc78210bd9433111f1332f549aa003f7f28cc6c68c038aa35b5ddfc98242a8ede07ea8fa073f2b0e7f1247b8b97fadc4837c91c");

        final WorkerpoolOrder signedWorkerpoolOrder = buildSignedWorkerpoolOrder(workerpoolAddress);
        assertThat(signedWorkerpoolOrder.getSign())
                .isEqualTo("0xf6474336a10e3ec12ff65e721fee6ba53763f937a1b54659332f61cf1770b4b26947df22ad89e8623aa985763e917e9f66670e51befc7195b16b0e8db365c13c1c");

        final RequestOrder signedRequestOrder = buildSignedRequestOrder(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder);
        assertThat(signedRequestOrder.getSign())
                .isEqualTo("0x6b38847f28bd7249823a7b99538ff6cca4a2cd952d5860f062be8142b3defcd13fad163a10e6de9e219981a333d3fbd554532be07263b7d77cbd48b2803413051c");

        nonce = nonce.add(BigInteger.ONE);
        String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData
        );
        TransactionReceipt receipt = null;
        while (receipt == null) {
            TimeUnit.SECONDS.sleep(1);
            receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        }

        assertThat(receipt).isNotNull();
        assertThat(receipt.isStatusOK()).isTrue();

        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);
        byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;

        String chainDealId = BytesUtils.bytesToString(dealid);
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainDealId);
        assertThat(oChainDeal).isPresent();
    }

    private AppOrder buildSignedAppOrder(String appAddress) {
        final AppOrder appOrder = AppOrder.builder()
                .app(appAddress)
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String("abcd"))
                .build();
        return signer.signAppOrder(appOrder);
    }

    private DatasetOrder buildSignedDatasetOrder(String datasetAddress) {
        final DatasetOrder datasetOrder = DatasetOrder.builder()
                .dataset(datasetAddress)
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String("abcd"))
                .build();
        return signer.signDatasetOrder(datasetOrder);
    }

    private WorkerpoolOrder buildSignedWorkerpoolOrder(String workerpoolAddress) {
        final WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
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
        return signer.signWorkerpoolOrder(workerpoolOrder);
    }

    private RequestOrder buildSignedRequestOrder(AppOrder appOrder, DatasetOrder datasetOrder, WorkerpoolOrder workerpoolOrder) {
        final TreeMap<String, String> iexecSecrets = new TreeMap<>(Map.of(
                "1", "first-secret",
                "2", "second-secret",
                "3", "third-secret"));
        final DealParams dealParams = DealParams.builder()
                .iexecResultEncryption(true)
                .iexecResultStorageProvider("ipfs")
                .iexecResultStorageProxy("http://result-proxy:13200")
                .iexecSecrets(iexecSecrets)
                .build();
        final RequestOrder requestOrder = RequestOrder.builder()
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
        return signer.signRequestOrder(requestOrder);
    }

    private String decodeReceipt(TransactionReceipt receipt) {
        log.info("receipt {}", receipt);
        return receipt.getLogs().stream()
                .findFirst()
                .map(log -> log.getTopics().get(3)) // dataset is an ERC721
                .map(Numeric::toBigInt)
                .map(addressBigInt -> Numeric.toHexStringWithPrefixZeroPadded(addressBigInt, 40))
                .orElse("");
    }

}
