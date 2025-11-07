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

import com.iexec.commons.poco.utils.BytesUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.web3j.utils.Numeric;

import java.util.Arrays;

import static com.iexec.commons.poco.chain.Web3jAbstractService.toBigInt;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PoCoDataDecoder {
    /**
     * Extracts 32 bytes hex strings from a raw data and converts them back to ASCII characters.
     *
     * @param parts  Arrays of 32 bytes hex strings
     * @param offset Where to find the first data which is the string length
     * @return The decoded string
     */
    public static String decodeToAsciiString(final String[] parts, final int offset) {
        return BytesUtils.hexStringToAscii(decodeToHexString(parts, offset));
    }

    /**
     * Extracts 32 bytes hex strings from a raw data.
     *
     * @param parts  Arrays of 32 bytes hex strings
     * @param offset Where to find the first data which is the string length
     * @return The extracted hex string
     */
    public static String decodeToHexString(final String[] parts, final int offset) {
        final int size = toBigInt(parts[offset]).intValue();
        log.debug("Size {}", size);
        final StringBuilder sb = new StringBuilder();
        int remainingSize = size;
        int chunk = 1;
        while (remainingSize >= 32) {
            sb.append(parts[offset + chunk]);
            remainingSize -= 32;
            chunk++;
        }
        if (remainingSize != 0) {
            sb.append(parts[offset + chunk], 0, 2 * remainingSize);
        }
        return sb.toString();
    }

    /**
     * Remove hex prefix and split every 32 bytes, i.e. every 64 hex characters.
     *
     * @param rawData Raw data to parse
     * @return array containing 32 bytes length hex strings
     */
    public static String[] toParts(final String rawData) {
        final String[] parts = Numeric.cleanHexPrefix(rawData).split("(?<=\\G.{64})");
        if (log.isTraceEnabled()) {
            log.trace("parts size {}", parts.length);
            Arrays.stream(parts).forEach(log::trace);
        }
        return parts;
    }
}
