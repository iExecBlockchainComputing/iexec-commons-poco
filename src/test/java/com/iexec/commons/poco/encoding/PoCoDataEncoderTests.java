/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.encoding;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.iexec.commons.poco.chain.Web3jAbstractService.GAS_LIMIT_CAP;
import static org.assertj.core.api.Assertions.assertThat;

class PoCoDataEncoderTests {
    @Test
    void getGasLimitForFunction() {
        assertThat(PoCoDataEncoder.getGasLimitForFunction("initialize"))
                .isEqualTo(BigInteger.valueOf(300_000));
        assertThat(PoCoDataEncoder.getGasLimitForFunction("contribute"))
                .isEqualTo(BigInteger.valueOf(500_000));
        assertThat(PoCoDataEncoder.getGasLimitForFunction("reveal"))
                .isEqualTo(BigInteger.valueOf(100_000));
        assertThat(PoCoDataEncoder.getGasLimitForFunction("finalize"))
                .isEqualTo(BigInteger.valueOf(500_000));
        assertThat(PoCoDataEncoder.getGasLimitForFunction("contributeAndFinalize"))
                .isEqualTo(BigInteger.valueOf(700_000));
        assertThat(PoCoDataEncoder.getGasLimitForFunction("reopen"))
                .isEqualTo(BigInteger.valueOf(500_000));
        assertThat(PoCoDataEncoder.getGasLimitForFunction("randomfunction"))
                .isEqualTo(BigInteger.valueOf(GAS_LIMIT_CAP));
    }
}
