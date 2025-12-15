/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.eip712.EIP712TypedData;
import com.iexec.commons.poco.encoding.MatchOrdersDataEncoder;
import com.iexec.commons.poco.order.*;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.iexec.commons.poco.encoding.MatchOrdersDataEncoder.encodeAssertDatasetDealCompatibility;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_NAME;
import static com.iexec.commons.poco.itest.ChainTests.SERVICE_PORT;
import static com.iexec.commons.poco.itest.IexecHubTestService.*;
import static com.iexec.commons.poco.itest.Web3jTestService.MINING_TIMEOUT;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@Slf4j
@Tag("itest")
@Testcontainers
class MatchOrdersTests {

    private static final String CHAIN_DEAL_ID = "0xc9b8098ec186899ae95cb349edabab76e6185625d4de5c82595147a7df458202";

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
        ordersService = new OrdersService(iexecHubService.getOrdersDomain(), signerService);
    }

    @Test
    void shouldBeConnectedToNode() {
        assertThat(web3jService.isConnected()).isTrue();
    }

    @Test
    void shouldFailAssertDatasetDealCompatibilityWhenBadlySignedWhenConsumed() throws IOException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();
        final DatasetOrder datasetOrder = getValidOrderBuilder(deployedAddresses.get("dataset")).volume(BigInteger.ZERO).build();
        final DatasetOrder signedDatasetOrder = (DatasetOrder) signerService.signOrderForDomain(datasetOrder, iexecHubService.getOrdersDomain());
        final String assertDatasetDealCompatibilityTxData = encodeAssertDatasetDealCompatibility(signedDatasetOrder, CHAIN_DEAL_ID);
        assertThatThrownBy(() -> sendCall(assertDatasetDealCompatibilityTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("Dataset order is revoked or fully consumed");
    }

    @Test
    void shouldFailAssertDatasetDealCompatibilityWhenBadlySigned() throws IOException, GeneralSecurityException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();
        final DatasetOrder datasetOrder = getValidOrderBuilder(deployedAddresses.get("dataset")).build();
        final SignerService otherSigner = new SignerService(null, web3jService.getChainId(), Credentials.create(Keys.createEcKeyPair()));
        final DatasetOrder signedDatasetOrder = (DatasetOrder) otherSigner.signOrderForDomain(datasetOrder, iexecHubService.getOrdersDomain());
        final String assertDatasetDealCompatibilityTxData = encodeAssertDatasetDealCompatibility(signedDatasetOrder, CHAIN_DEAL_ID);
        assertThatThrownBy(() -> sendCall(assertDatasetDealCompatibilityTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("Invalid dataset order signature");
    }

    @Test
    void shouldFailAssertDatasetCompatibilityWhenDealNotFound() throws IOException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();
        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(deployedAddresses.get("dataset"));
        final String assertDatasetDealCompatibilityTxData = encodeAssertDatasetDealCompatibility(signedDatasetOrder, CHAIN_DEAL_ID);
        assertThatThrownBy(() -> sendCall(assertDatasetDealCompatibilityTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("Deal not found");
    }

    @Test
    void shouldMatchOrdersWithDataset() throws IOException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();

        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(deployedAddresses.get("app"));
        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(deployedAddresses.get("dataset"));
        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(deployedAddresses.get("workerpool"), BigInteger.ONE);
        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, BigInteger.ONE);

        final BigInteger nonce = signerService.getNonce();
        final String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        final String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(matchOrdersTxHash));
        assertThat(web3jService.areTxStatusOK(matchOrdersTxHash)).isTrue();

        final TransactionReceipt receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);

        final byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;
        final String chainDealId = BytesUtils.bytesToString(dealid);
        assertThat(iexecHubService.getChainDeal(chainDealId)).isPresent();
        assertThat(iexecHubService.getChainDealWithDetails(chainDealId)).isPresent();

        // Assets ownership
        for (final String predictedAssetAddress : List.of(deployedAddresses.get("app"), deployedAddresses.get("dataset"), deployedAddresses.get("workerpool"))) {
            assertThat(iexecHubService.getOwner(predictedAssetAddress)).isEqualTo(signerService.getAddress());
        }

        // matchOrders transaction and logs
        assertThat(web3jService.getTransactionByHash(matchOrdersTxHash)).isNotNull();
        assertThat(iexecHubService.fetchLogTopics(matchOrdersTxHash))
                .isEqualTo(List.of("Transfer", "Lock", "Transfer", "Lock", "SchedulerNotice", "OrdersMatched"));

        // orders consumption
        for (final EIP712TypedData typedData : List.of(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder)) {
            assertThat(iexecHubService.viewConsumed(typedData.computeHash(iexecHubService.getOrdersDomain()))).isOne();
        }

        // estimate gas revert telling us that not enough volumes are available across the 4 orders
        // https://github.com/iExecBlockchainComputing/PoCo/blob/v6.0.0/contracts/facets/IexecPoco1Facet.sol#L311
        assertThatThrownBy(() -> signerService.estimateGas(IEXEC_HUB_ADDRESS, matchOrdersTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("iExecV5-matchOrders-0x60");

        // assertDatasetDealCompatibility reverts for fully consumed dataset
        final String assertDatasetDealCompatibilityTxData = encodeAssertDatasetDealCompatibility(signedDatasetOrder, chainDealId);
        assertThatThrownBy(() -> sendCall(assertDatasetDealCompatibilityTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("Dataset order is revoked or fully consumed");
        // assertDatasetDealCompatibility reverts if deal has a dataset
        final DatasetOrder invalidDatasetOrder = getValidOrderBuilder(deployedAddresses.get("dataset")).build();
        final DatasetOrder signedInvalidDatasetOrder = (DatasetOrder) signerService.signOrderForDomain(invalidDatasetOrder, iexecHubService.getOrdersDomain());
        final String txData = MatchOrdersDataEncoder.encodeAssertDatasetDealCompatibility(signedInvalidDatasetOrder, chainDealId);
        assertThatThrownBy(() -> sendCall(txData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("Deal already has a dataset");
    }

    @Test
    void shouldMatchOrdersWithoutDataset() throws IOException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();

        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(deployedAddresses.get("app"));
        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(BytesUtils.EMPTY_ADDRESS);
        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(deployedAddresses.get("workerpool"), BigInteger.ONE);
        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, BigInteger.ONE);

        final BigInteger nonce = signerService.getNonce();
        final String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        final String matchOrdersTxHash = signerService.signAndSendTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, IEXEC_HUB_ADDRESS, matchOrdersTxData);

        await().atMost(MINING_TIMEOUT, TimeUnit.SECONDS)
                .until(() -> web3jService.areTxMined(matchOrdersTxHash));
        assertThat(web3jService.areTxStatusOK(matchOrdersTxHash)).isTrue();

        final TransactionReceipt receipt = web3jService.getTransactionReceipt(matchOrdersTxHash);
        assertThat(IexecHubContract.getOrdersMatchedEvents(receipt)).hasSize(1);

        final byte[] dealid = IexecHubContract.getOrdersMatchedEvents(receipt).get(0).dealid;
        final String chainDealId = BytesUtils.bytesToString(dealid);
        assertThat(iexecHubService.getChainDeal(chainDealId)).isPresent();
        assertThat(iexecHubService.getChainDealWithDetails(chainDealId)).isPresent();

        // matchOrders transaction and logs
        assertThat(web3jService.getTransactionByHash(matchOrdersTxHash)).isNotNull();
        assertThat(iexecHubService.fetchLogTopics(matchOrdersTxHash))
                .isEqualTo(List.of("Transfer", "Lock", "Transfer", "Lock", "SchedulerNotice", "OrdersMatched"));

        // estimate gas revert telling us that not enough volumes are available across the 4 orders
        // https://github.com/iExecBlockchainComputing/PoCo/blob/v6.0.0/contracts/facets/IexecPoco1Facet.sol#L311
        assertThatThrownBy(() -> signerService.estimateGas(IEXEC_HUB_ADDRESS, matchOrdersTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("iExecV5-matchOrders-0x60");

        // assertDatasetDealCompatibility revert when dataset not present
        final String assertDatasetDealCompatibilityTxData = encodeAssertDatasetDealCompatibility(signedDatasetOrder, chainDealId);
        assertThatThrownBy(() -> sendCall(assertDatasetDealCompatibilityTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("\"revert\"");

        // assertDatasetDealCompatibility checks for a deal without a dataset
        for (final Map.Entry<DatasetOrder, String> entry : getInvalidOrders(deployedAddresses.get("dataset")).entrySet()) {
            final DatasetOrder signedInvalidDatasetOrder = (DatasetOrder) signerService.signOrderForDomain(entry.getKey(), iexecHubService.getOrdersDomain());
            final String assertCompatibilityTxData = encodeAssertDatasetDealCompatibility(signedInvalidDatasetOrder, chainDealId);
            assertThatThrownBy(() -> sendCall(assertCompatibilityTxData), entry.getValue())
                    .isInstanceOf(JsonRpcError.class)
                    .hasMessage(entry.getValue());
        }
    }

    @ParameterizedTest
    @MethodSource("provideValidTags")
    void shouldMatchOrdersWithValidTags(final String appTag, final String datasetTag, final String requestTag, final String workerpoolTag) throws IOException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();
        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(deployedAddresses.get("app"), formatTag(appTag));
        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(deployedAddresses.get("dataset"), formatTag(datasetTag));
        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(deployedAddresses.get("workerpool"), BigInteger.ONE, formatTag(workerpoolTag));
        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, BigInteger.ONE, formatTag(requestTag));

        final String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        assertThatNoException().isThrownBy(() -> signerService.estimateGas(IEXEC_HUB_ADDRESS, matchOrdersTxData));
    }

    static Stream<Arguments> provideValidTags() {
        return Stream.of(
                Arguments.of("0x1", "0x1", "0x1", "0x1"),
                Arguments.of("0x1", "0x1", "0x0", "0x1"),
                Arguments.of("0x1", "0x0", "0x1", "0x1"),
                Arguments.of("0x1", "0x0", "0x0", "0x1"),
                Arguments.of("0x0", "0x0", "0x0", "0x1"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTags")
    void shouldNotMatchOrdersWithInvalidTags(final String appTag, final String datasetTag, final String requestTag, final String workerpoolTag) throws IOException {
        final Map<String, String> deployedAddresses = iexecHubService.deployAssets();
        final AppOrder signedAppOrder = ordersService.buildSignedAppOrder(deployedAddresses.get("app"), formatTag(appTag));
        final DatasetOrder signedDatasetOrder = ordersService.buildSignedDatasetOrder(deployedAddresses.get("dataset"), formatTag(datasetTag));
        final WorkerpoolOrder signedWorkerpoolOrder = ordersService.buildSignedWorkerpoolOrder(deployedAddresses.get("workerpool"), BigInteger.ONE, formatTag(workerpoolTag));
        final RequestOrder signedRequestOrder = ordersService.buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, BigInteger.ONE, formatTag(requestTag));

        final String matchOrdersTxData = MatchOrdersDataEncoder.encode(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
        assertThatThrownBy(() -> signerService.estimateGas(IEXEC_HUB_ADDRESS, matchOrdersTxData))
                .isInstanceOf(JsonRpcError.class)
                .hasMessage("iExecV5-matchOrders-0x07");
    }

    private String formatTag(final String hexTag) {
        return Numeric.toHexStringWithPrefixZeroPadded(Numeric.toBigInt(hexTag), 64);
    }

    static Stream<Arguments> provideInvalidTags() {
        return Stream.of(
                Arguments.of("0x0", "0x0", "0x1", "0x1"),
                Arguments.of("0x0", "0x1", "0x0", "0x1"));
    }

    // region utils
    private DatasetOrder.DatasetOrderBuilder getValidOrderBuilder(final String datasetAddress) {
        return DatasetOrder.builder()
                .dataset(datasetAddress)
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)));

    }

    private Map<DatasetOrder, String> getInvalidOrders(final String datasetAddress) {
        return Map.ofEntries(
                Map.entry(getValidOrderBuilder(datasetAddress).volume(BigInteger.ZERO).build(), "Dataset order is revoked or fully consumed"),
                Map.entry(getValidOrderBuilder(datasetAddress).apprestrict(IEXEC_HUB_ADDRESS).build(), "App restriction not satisfied"),
                Map.entry(getValidOrderBuilder(datasetAddress).workerpoolrestrict(IEXEC_HUB_ADDRESS).build(), "Workerpool restriction not satisfied"),
                Map.entry(getValidOrderBuilder(datasetAddress).requesterrestrict(IEXEC_HUB_ADDRESS).build(), "Requester restriction not satisfied"),
                Map.entry(getValidOrderBuilder(datasetAddress).tag("0xFF").build(), "Tag compatibility not satisfied")
        );
    }

    private void sendCall(final String txData) throws IOException {
        web3jService.sendCall(signerService.getAddress(), IEXEC_HUB_ADDRESS, txData);
    }
    // endregion

}
