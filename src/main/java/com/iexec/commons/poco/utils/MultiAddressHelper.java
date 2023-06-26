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

import io.ipfs.multiaddr.MultiAddress;
import org.apache.commons.lang3.StringUtils;
import org.web3j.utils.Numeric;

import java.util.List;

public class MultiAddressHelper {

    public static final List<String> IPFS_GATEWAYS = List.of(
            "https://ipfs-gateway.v8-bellecour.iex.ec",
            "https://gateway.ipfs.io",
            "https://gateway.pinata.cloud"
    );

    private MultiAddressHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts the hexadecimal String stored in an on-chain deal to a human-readable format.
     * <p>
     * IPFS addresses are stored in a specific format and have to be converted with the {@link MultiAddress} constructor.
     * If a {@link MultiAddress} instance can be constructed, the URI it represents has been successfully converted.
     * In other cases, the value is considered to be a string encoded in hexadecimal and will be converted with
     * {@link BytesUtils#hexStringToAscii(String)}.
     *
     * @param hexaString String to convert
     * @return Conversion result
     */
    public static String convertToURI(String hexaString) {
        try {
            return new MultiAddress(Numeric.hexStringToByteArray(hexaString)).toString();
        } catch (Exception e) {
            return BytesUtils.hexStringToAscii(hexaString);
        }
    }

    public static boolean isMultiAddress(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        try {
            new MultiAddress(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
