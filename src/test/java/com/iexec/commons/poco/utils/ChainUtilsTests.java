/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.utils;

import org.junit.jupiter.api.Test;

import static com.iexec.commons.poco.chain.ChainUtils.generateChainTaskId;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainUtilsTests {

    @Test
    void shouldBeCorrectOneValue(){
        String dealId = "0xa0b0fd396b0f79f14e4d6b34af7180bd9e80e2d86afda91c6127c5c17a268e66";
        String chainTaskId = generateChainTaskId(dealId, 0);

        String expectedChainTaskId = "0xe06c86d6bb750dbd9be9e002482854b3b3f21550dbe37236767b9cac29e3ce28";
        assertEquals(chainTaskId, expectedChainTaskId);
    }

}
