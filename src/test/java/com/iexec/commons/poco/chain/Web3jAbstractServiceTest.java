/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.chain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Web3jAbstractServiceTest {

    private final int chainId = 65535;
    private final String nodeAddress = "http://localhost:8545";
    private final Duration blockTime = Duration.ofSeconds(5);
    private final float gasPriceMultiplier = 1.0f;
    private final long gasPriceCap = 1_000_000L;
    private final boolean isSidechain = true;

    private final Web3jAbstractService web3jAbstractService =
            new Web3jAbstractService(chainId, nodeAddress, blockTime, gasPriceMultiplier, gasPriceCap, isSidechain) {
            };

    @Test
    void shouldCreateInstance() {
        assertDoesNotThrow(
                () -> new Web3jAbstractService(chainId, nodeAddress, blockTime, gasPriceMultiplier, gasPriceCap, isSidechain) {
                });
    }

    @Test
    void shouldNotCreateInstanceWhenNullBlockTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new Web3jAbstractService(chainId, nodeAddress, null, gasPriceMultiplier, gasPriceCap, isSidechain) {
                });
    }

    @Test
    void shouldNotCreateInstanceWhenNegativeBlockTime() {
        final Duration negativeBlockTime = Duration.ofSeconds(-1);
        assertThrows(IllegalArgumentException.class,
                () -> new Web3jAbstractService(chainId, nodeAddress, negativeBlockTime, gasPriceMultiplier, gasPriceCap, isSidechain) {
                });
    }

    // region checkConnection
    @Test
    void shouldNotBeConnected() {
        assertThat(web3jAbstractService.isConnected()).isFalse();
    }
    // endregion

}
