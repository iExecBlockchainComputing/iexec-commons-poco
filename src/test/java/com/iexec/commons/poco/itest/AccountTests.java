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

import com.iexec.commons.poco.chain.ChainAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("itest")
@Testcontainers
class AccountTests {

    private Credentials credentials;
    private IexecHubService iexecHubService;

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(new File("docker-compose.yml"))
            .withExposedService("poco-chain", 8545);


    @BeforeEach
    void init() throws CipherException, IOException {
        credentials = WalletUtils.loadCredentials("whatever", "src/test/resources/wallet.json");
        Web3jService web3jService = new Web3jService(environment.getServicePort("poco-chain", 8545));
        iexecHubService = new IexecHubService(credentials, web3jService);
    }

    @Test
    void shouldGetAccount() {
        Optional<ChainAccount> oChainAccount = iexecHubService.getChainAccount(credentials.getAddress());
        assertThat(oChainAccount).isPresent();
        assertThat(oChainAccount.get().getDeposit()).isEqualTo(10_000_000L);
        assertThat(oChainAccount.get().getLocked()).isZero();
    }

}
