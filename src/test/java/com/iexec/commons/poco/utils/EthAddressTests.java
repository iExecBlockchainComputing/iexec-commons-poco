/*
 * Copyright 2021-2024 IEXEC BLOCKCHAIN TECH
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EthAddressTests {
    private static final String VALID_CHECKSUM_ADDRESS = "0x1Ec09E1782a43a770D54e813379c730E0b29aD4B";

    @Test
    void shouldValidateAddress() {
        // valid lowercase address
        assertTrue(EthAddress.validate(VALID_CHECKSUM_ADDRESS.toLowerCase()));
        // valid uppercase address
        assertTrue(EthAddress.validate(VALID_CHECKSUM_ADDRESS.toUpperCase().replace("0X", "0x")));
        // valid checksum address
        assertTrue(EthAddress.validate(VALID_CHECKSUM_ADDRESS));
    }

    @Test
    void shouldNotValidateAddress() {
        // invalid empty address
        assertFalse(EthAddress.validate(""));
        // invalid non-hex address
        assertFalse(EthAddress.validate(VALID_CHECKSUM_ADDRESS.replace("c", "z")));
        // invalid non-alphanumeric address
        assertFalse(EthAddress.validate(VALID_CHECKSUM_ADDRESS.replace("c", "&")));
        // invalid length address
        assertFalse(EthAddress.validate(VALID_CHECKSUM_ADDRESS.substring(0, 41)));
        // invalid checksum address
        assertFalse(EthAddress.validate(VALID_CHECKSUM_ADDRESS.replace('E', 'e')));
    }
}
