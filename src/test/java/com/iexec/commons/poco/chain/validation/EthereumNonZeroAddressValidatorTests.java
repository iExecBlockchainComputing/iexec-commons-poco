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

package com.iexec.commons.poco.chain.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

class EthereumNonZeroAddressValidatorTests {

    @Test
    void shouldValidateAddress() {
        assertViolations("0x0000000000000000000000000000000000000001", 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0x0000000000000000000000000000000000000000",
            ""
    })
    void shouldNotValidateAddress(String address) {
        assertViolations(address, 1);
    }

    private void assertViolations(String address, int i) {
        final Account account = new Account(address);
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            final Set<ConstraintViolation<Account>> violations = validator.validate(account);
            Assertions.assertEquals(i, violations.size());
        }
    }

    static class Account {

        @ValidNonZeroEthereumAddress
        private final String address;

        public Account(String address) {
            this.address = address;
        }
    }
}
