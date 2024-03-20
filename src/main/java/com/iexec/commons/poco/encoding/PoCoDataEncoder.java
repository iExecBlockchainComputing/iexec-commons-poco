/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static com.iexec.commons.poco.encoding.Utils.toHexString;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PoCoDataEncoder {

    private static final String INITIALIZE_SELECTOR = "0x5b36c66b";
    private static final String CONTRIBUTE_SELECTOR = "0x34623484";
    private static final String REVEAL_SELECTOR = "0xfc334e8c";
    private static final String FINALIZE_SELECTOR = "0x8fc375e5";
    private static final String CONTRIBUTE_AND_FINALIZE_SELECTOR = "0x5facd761";

    public static String encodeInitialize(String dealid, int idx) {
        return INITIALIZE_SELECTOR +
                toHexString(dealid) +
                toHexString(BigInteger.valueOf(idx));
    }

    public static String encodeContribute(String chainTaskId, String resultHash, String resultSeal, String enclaveChallenge, String enclaveSign, String authorizationSign) {
        final long enclaveSignOffset = 6 * 32L;
        final String enclaveSignContrib = TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(enclaveSign)));

        final long authorizationOffset = enclaveSignOffset + enclaveSignContrib.length() / 64 * 32L;
        final String authorizationContrib = TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(authorizationSign)));

        return CONTRIBUTE_SELECTOR +
                toHexString(chainTaskId) +
                toHexString(resultHash) +
                toHexString(resultSeal) +
                toHexString(enclaveChallenge) +
                toHexString(BigInteger.valueOf(enclaveSignOffset)) +
                toHexString(BigInteger.valueOf(authorizationOffset)) +
                enclaveSignContrib +
                authorizationContrib;
    }

    public static String encodeReveal(String chainTaskId, String resultDigest) {
        return REVEAL_SELECTOR +
                toHexString(chainTaskId) +
                toHexString(resultDigest);
    }

    public static String encodeFinalize(String chainTaskId, byte[] results, byte[] resultsCallback) {
        final long resultsOffset = 3 * 32L;
        final String resultsContrib = TypeEncoder.encode(new DynamicBytes(results));

        final long resultsCallbackOffset = resultsOffset + resultsContrib.length() / 64 * 32L;
        final String resultsCallbackContrib = TypeEncoder.encode(new DynamicBytes(resultsCallback));

        return FINALIZE_SELECTOR +
                toHexString(chainTaskId) +
                toHexString(BigInteger.valueOf(resultsOffset)) +
                toHexString(BigInteger.valueOf(resultsCallbackOffset)) +
                resultsContrib +
                resultsCallbackContrib;
    }

    public static String encodeContributeAndFinalize(String chainTaskId, String resultDigest, byte[] results, byte[] resultsCallback, String enclaveChallenge, String enclaveSign, String authorizationSign) {
        final long resultsOffset = 7 * 32L;
        final String resultsContrib = TypeEncoder.encode(new DynamicBytes(results));

        final long resultsCallbackOffset = resultsOffset + resultsContrib.length() / 64 * 32L;
        final String resultsCallbackContrib = TypeEncoder.encode(new DynamicBytes(resultsCallback));

        final long enclaveSignOffset = resultsCallbackOffset + resultsCallbackContrib.length() / 64 * 32L;
        final String enclaveSignContrib = TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(enclaveSign)));

        final long authorizationSignOffset = enclaveSignOffset + enclaveSignContrib.length() / 64 * 32L;
        final String authorizationSignContrib = TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(authorizationSign)));

        return CONTRIBUTE_AND_FINALIZE_SELECTOR +
                toHexString(chainTaskId) +
                toHexString(resultDigest) +
                toHexString(BigInteger.valueOf(resultsOffset)) +
                toHexString(BigInteger.valueOf(resultsCallbackOffset)) +
                toHexString(enclaveChallenge) +
                toHexString(BigInteger.valueOf(enclaveSignOffset)) +
                toHexString(BigInteger.valueOf(authorizationSignOffset)) +
                resultsContrib +
                resultsCallbackContrib +
                enclaveSignContrib +
                authorizationSignContrib;
    }

}
