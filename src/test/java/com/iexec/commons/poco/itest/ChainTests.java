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

import com.iexec.commons.poco.chain.ChainAccount;
import com.iexec.commons.poco.chain.ChainCategory;
import com.iexec.commons.poco.contract.IexecHubSmartContractValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

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
        this.web3jService = new Web3jTestService(chainNodeAddress);
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
    void shouldGetBlockNumber() throws IOException {
        final long blockNumber = web3jService.getLatestBlockNumber();
        final EthBlock.Block block = web3jService.getLatestBlock();
        assertThat(blockNumber).isNotZero();
        assertThat(block.getNumber().longValue()).isBetween(blockNumber, blockNumber + 1);
        assertThat(web3jService.getBlock(block.getNumber().longValue())).isEqualTo(block);
    }

    @Test
    void shouldNotGetBlockNumber() {
        final Web3jTestService badWeb3jService = new Web3jTestService(badBlockchainAddress);
        assertThat(badWeb3jService.getLatestBlockNumber()).isZero();
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

    @Test
    void shouldNotGetChainDeal() {
        assertThat(iexecHubService.getChainDeal("0x0")).isEmpty();
        assertThat(iexecHubService.getChainDealWithDetails("0x0")).isEmpty();
    }

    @Test
    void shouldNotGetChainTask() {
        assertThat(iexecHubService.getChainTask("0x0")).isEmpty();
    }

    @Test
    void shouldValidateSmartContract() {
        assertThat(IexecHubSmartContractValidator.validate(iexecHubService.getHubContract())).isTrue();
    }

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
        final Web3jTestService badWeb3jService = new Web3jTestService(badBlockchainAddress);
        assertThat(badWeb3jService.getUserGasPrice()).isEqualTo(22_000_000_000L);
    }
    // endregion

}
