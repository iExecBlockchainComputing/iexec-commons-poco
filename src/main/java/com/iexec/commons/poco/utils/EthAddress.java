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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Keys;

import java.util.regex.Pattern;

/**
 * Account utility functions.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EthAddress {

    private static final String IGNORE_CASE_ADDRESS_PATTERN = "(?i)^(0x)?[0-9a-f]{40}$";
    private static final String LOWERCASE_ADDRESS_PATTERN = "^(0x)?[0-9a-f]{40}$";
    private static final String UPPERCASE_ADDRESS_PATTERN = "^(0x)?[0-9A-F]{40}$";

    /**
     * Check if the given string is a valid Ethereum address. Inspired by
     * the web3js implementation.
     *
     * @param address in hex
     * @return true if address is valid, false otherwise
     * @see <a href="https://github.com/ChainSafe/web3.js/blob/5d027191c5cb7ffbcd44083528bdab19b4e14744/packages/web3-utils/src/utils.js#L88">
     * web3.js isAddress implementation</a>
     */
    public static boolean validate(String address) {
        // check address is not empty and contains the valid
        // number (40 without 0x) and type (alphanumeric) of characters
        if (StringUtils.isEmpty(address) ||
                !matchesRegex(address, IGNORE_CASE_ADDRESS_PATTERN)) {
            return false;
        }
        // check for all upper/lower case
        if (matchesRegex(address, LOWERCASE_ADDRESS_PATTERN)
                || matchesRegex(address, UPPERCASE_ADDRESS_PATTERN)) {
            return true;
        }
        // validate checksum address when mixed case
        return Keys.toChecksumAddress(address).equals(address);
    }

    private static boolean matchesRegex(String address, String regex) {
        return Pattern.compile(regex).matcher(address).find();
    }
}
