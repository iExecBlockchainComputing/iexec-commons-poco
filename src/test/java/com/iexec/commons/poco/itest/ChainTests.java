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

import com.iexec.commons.poco.chain.ChainAccount;
import com.iexec.commons.poco.chain.ChainCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.iexec.commons.poco.itest.IexecHubTestService.IEXEC_HUB_ADDRESS;
import static com.iexec.commons.poco.utils.BytesUtils.EMPTY_ADDRESS;
import static com.iexec.commons.poco.utils.BytesUtils.EMPTY_HEX_STRING_32;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("itest")
@Testcontainers
class ChainTests {

    static final String SERVICE_NAME = "poco-chain";
    static final int SERVICE_PORT = 8545;

    private final String badBlockchainAddress = "http://localhost:5458";

    private Credentials credentials;
    private IexecHubTestService iexecHubService;
    private Web3jTestService web3jService;
    private String chainNodeAddress;

    @Container
    static ComposeContainer environment = new ComposeContainer(new File("docker-compose.yml"))
            .withPull(true)
            .withExposedService(SERVICE_NAME, SERVICE_PORT);


    @BeforeEach
    void init() throws CipherException, IOException {
        this.credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        this.chainNodeAddress = "http://" + environment.getServiceHost(SERVICE_NAME, SERVICE_PORT) + ":" +
                environment.getServicePort(SERVICE_NAME, SERVICE_PORT);
        this.web3jService = new Web3jTestService(chainNodeAddress, 1.0f, 22_000_000_000L);
        this.iexecHubService = new IexecHubTestService(credentials, web3jService);
    }

    @Test
    void shouldGetAccount() {
        final ChainAccount chainAccount = iexecHubService.getChainAccount(credentials.getAddress()).orElse(null);
        assertThat(chainAccount).isNotNull();
        assertThat(chainAccount.getDeposit()).isEqualTo(40_178L);
        assertThat(chainAccount.getLocked()).isZero();
    }

    @Test
    void shouldGetBalance() {
        final BigInteger balance = web3jService.getBalance(credentials.getAddress()).orElse(null);
        assertThat(balance).isEqualTo(new BigInteger("3188369135434504514964210500676909925639291603846501657344"));
    }

    @Test
    void shouldNotGetBalance() {
        final Web3jTestService badWeb3jService = new Web3jTestService(badBlockchainAddress);
        assertThat(badWeb3jService.getBalance(credentials.getAddress())).isEmpty();
    }

    @Test
    void shouldGetBlockNumber() {
        final long blockNumber = web3jService.getLatestBlockNumber();
        assertThat(blockNumber).isPositive();
    }

    @Test
    void shouldNotGetBlockNumber() {
        final Web3jTestService badWeb3jService = new Web3jTestService(badBlockchainAddress);
        assertThat(badWeb3jService.getLatestBlockNumber()).isZero();
    }

    @Test
    void shouldGetCallbackGas() throws IOException {
        final BigInteger callbackGas = iexecHubService.getCallbackGas();
        assertThat(callbackGas).isEqualTo(BigInteger.valueOf(200_000));
    }

    @Test
    void shouldGetContributionDeadlineRatio() throws IOException {
        final BigInteger contributionDeadlineRatio = iexecHubService.getContributionDeadlineRatio();
        assertThat(contributionDeadlineRatio).isEqualTo(BigInteger.valueOf(7));
    }

    @Test
    void shouldGetFinalDeadlineRatio() throws IOException {
        final BigInteger finalDeadlineRatio = iexecHubService.getFinalDeadlineRatio();
        assertThat(finalDeadlineRatio).isEqualTo(BigInteger.valueOf(10));
    }

    @Test
    void shouldGetOwner() {
        final String owner = iexecHubService.getOwner(IEXEC_HUB_ADDRESS);
        assertThat(owner).isEqualTo("0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266");
    }

    @Test
    void shouldNotGetOwner() {
        assertThat(iexecHubService.getOwner(EMPTY_ADDRESS)).isEmpty();
        assertThat(iexecHubService.getOwner(credentials.getAddress())).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("categoryProvider")
    void shouldGetCategory(long id, ChainCategory expectedCategory) {
        final ChainCategory chainCategory = iexecHubService.getChainCategory(id).orElse(null);
        assertThat(chainCategory).isEqualTo(expectedCategory);
    }

    private static Stream<Arguments> categoryProvider() {
        return Stream.of(
                Arguments.of(0, ChainCategory.builder().id(0).name("XS").description("{}").maxExecutionTime(300_000L).build()),
                Arguments.of(1, ChainCategory.builder().id(1).name("S").description("{}").maxExecutionTime(1_200_000L).build()),
                Arguments.of(2, ChainCategory.builder().id(2).name("M").description("{}").maxExecutionTime(3_600_000L).build()),
                Arguments.of(3, ChainCategory.builder().id(3).name("L").description("{}").maxExecutionTime(10_800_000L).build()),
                Arguments.of(4, ChainCategory.builder().id(4).name("XL").description("{}").maxExecutionTime(36_000_000L).build())
        );
    }

    @Test
    void shouldGetMaxNbOfPeriodsForConsensus() {
        final long maxNbOfPeriodsForConsensus = iexecHubService.getMaxNbOfPeriodsForConsensus();
        assertThat(maxNbOfPeriodsForConsensus).isEqualTo(7L);
    }

    @Test
    void shouldGetWorkerScore() {
        final Integer score = iexecHubService.getWorkerScore(credentials.getAddress()).orElse(null);
        assertThat(score).isZero();
    }

    @Test
    void shouldHaveEnoughGas() {
        assertThat(web3jService.hasEnoughGas(credentials.getAddress())).isTrue();
    }

    // region deal and task

    @Test
    void shouldNotGetChainDeal() {
        assertThat(iexecHubService.getChainDeal(EMPTY_HEX_STRING_32)).isEmpty();
        assertThat(iexecHubService.getChainDealWithDetails(EMPTY_HEX_STRING_32)).isEmpty();
    }

    @Test
    void shouldNotGetChainTask() {
        assertThat(iexecHubService.getChainTask(EMPTY_HEX_STRING_32)).isEmpty();
    }

    // endregion

    // region getTransactionByHash

    @Test
    void shouldNotGetTransactionByHashOnEmptyHash() {
        assertThat(web3jService.getTransactionByHash(EMPTY_HEX_STRING_32)).isNull();
    }

    @Test
    void shouldNotGetTransactionByHashOnBlockchainCommunicationError() {
        final Web3jTestService badWeb3jService = new Web3jTestService(badBlockchainAddress, 1.0f, 22_000_000_000L);
        assertThat(badWeb3jService.getTransactionByHash(EMPTY_HEX_STRING_32)).isNull();
    }

    // endregion

    // region gas price
    @Test
    void shouldGetNetworkGasPrice() {
        assertThat(web3jService.getNetworkGasPrice()).isEmpty();
    }

    @Test
    void shouldReturnNetworkPriceWhenBelowGasPriceCap() {
        assertThat(web3jService.getUserGasPrice()).isEqualTo(22_000_000_000L);
    }

    @Test
    void shouldReturnUserPriceCapWhenBelowNetworkPrice() {
        final float gasPriceMultiplier = 1.2f;
        final long gasPriceCap = 5_000_000_000L;
        final Web3jTestService newService = new Web3jTestService(chainNodeAddress, gasPriceMultiplier, gasPriceCap);
        assertThat(newService.getUserGasPrice()).isEqualTo(gasPriceCap);
    }

    @Test
    void shouldReturnGasPriceCapOnBlockchainCommunicationError() {
        final Web3jTestService badWeb3jService = new Web3jTestService(badBlockchainAddress, 1.0f, 22_000_000_000L);
        assertThat(badWeb3jService.getUserGasPrice()).isEqualTo(22_000_000_000L);
    }
    // endregion

    // region repeatCheck
    @Test
    void shouldSucceedToCheck() {
        ReflectionTestUtils.setField(web3jService, "blockTime", Duration.ofMillis(100));
        final Predicate<String[]> predicate = a -> a[0].contains(a[1]);
        final boolean result = web3jService.repeatCheck(
                0, 0, "check", predicate, "empty", "mpt");
        assertThat(result).isTrue();
    }

    @Test
    void shouldFailToCheck() {
        ReflectionTestUtils.setField(web3jService, "blockTime", Duration.ofMillis(100));
        final Predicate<String[]> predicate = a -> a[0].contains(a[1]);
        final boolean result = web3jService.repeatCheck(
                0, 0, "check", predicate, "empty", "tpm");
        assertThat(result).isFalse();
    }
    // endregion

}
