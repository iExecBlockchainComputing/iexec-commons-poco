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

 
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class HashUtils {

    private HashUtils() {
        throw new UnsupportedOperationException();
    }

    public static String concatenateAndHash(String... hexaString) {
        // convert
        byte[] res = new byte[0];
        for (String str : hexaString) {
            res = org.bouncycastle.util.Arrays.concatenate(res, BytesUtils.stringToBytes(str));
        }
        // Hash the result and convert to String
        return Numeric.toHexString(Hash.sha3(res));
    }
 
    /**
     * Generates SHA-256 digest for the given UTF-8 string.
     * 
     * @param utf8String
     * @return SHA-256 digest in hex string format,
     * e.g: 0x66daf4e6810d83d4859846a5df1afabf88c9fda135bc732ea977f25348d98ede
     */
    public static String sha256(String utf8String) {
        Objects.requireNonNull(utf8String, "UTF-8 string must not be null");
        byte[] bytes = utf8String.getBytes(StandardCharsets.UTF_8);
        return sha256(bytes);
    }

    /**
     * Generates SHA-256 digest for the given byte array.
     * 
     * @param bytes
     * @return SHA-256 digest in hex string format,
     * e.g: 0x66daf4e6810d83d4859846a5df1afabf88c9fda135bc732ea977f25348d98ede
     */
    public static String sha256(byte[] bytes) {
        byte[] hexBytes = Hash.sha256(bytes);
        return Numeric.toHexString(hexBytes);
    }
 
}
