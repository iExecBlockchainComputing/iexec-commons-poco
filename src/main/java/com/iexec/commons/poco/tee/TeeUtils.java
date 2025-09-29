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

package com.iexec.commons.poco.tee;

import com.iexec.commons.poco.utils.BytesUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TeeUtils {

    public static final int TEE_SCONE_BITS = 0b0011;
    public static final int TEE_GRAMINE_BITS = 0b0101;
    public static final int TEE_TDX_BITS = 0b1001;
    private static final Map<Integer, TeeFramework> TEE_BITS_TO_FRAMEWORK = Map.of(
            TEE_SCONE_BITS, TeeFramework.SCONE,
            TEE_GRAMINE_BITS, TeeFramework.GRAMINE,
            TEE_TDX_BITS, TeeFramework.TDX
    );
    public static final String TEE_SCONE_ONLY_TAG = BytesUtils.toByte32HexString(TEE_SCONE_BITS);
    public static final String TEE_GRAMINE_ONLY_TAG = BytesUtils.toByte32HexString(TEE_GRAMINE_BITS);
    public static final String TEE_TDX_ONLY_TAG = BytesUtils.toByte32HexString(TEE_TDX_BITS);
    private static final int TEE_RUNTIME_FRAMEWORK_MASK = 0b1111; //last nibble (4 bits)

    /**
     * Check if hexTag asks for a known TEE runtime framework.
     * <p>
     * To avoid breaking change, this will only deal with SGX
     *
     * @param hexTag tag of the deal
     * @return true if a known TEE runtime framework is requested
     * @deprecated for dedicated methods in {@code TaskDescription}
     */
    @Deprecated(forRemoval = true)
    public static boolean isTeeTag(final String hexTag) {
        return hasTeeSconeInTag(hexTag) || hasTeeGramineInTag(hexTag);
    }

    /**
     * Check if tag asks for Scone TEE framework.
     *
     * @param hexTag tag of the deal
     * @return true if Scone TEE framework is requested
     */
    static boolean hasTeeSconeInTag(final String hexTag) {
        return hasTeeRuntimeFrameworkBitsInTag(TEE_SCONE_BITS, hexTag);
    }

    /**
     * Check if tag asks for Gramine TEE framework.
     *
     * @param hexTag tag of the deal
     * @return true if Gramine TEE framework is requested
     */
    static boolean hasTeeGramineInTag(final String hexTag) {
        return hasTeeRuntimeFrameworkBitsInTag(TEE_GRAMINE_BITS, hexTag);
    }

    /**
     * Check if tag asks for TDX TEE framework.
     *
     * @param hexTag tag of the deal
     * @return true if TDX TEE framework is requested
     */
    static boolean hasTeeTdxInTag(final String hexTag) {
        return hasTeeRuntimeFrameworkBitsInTag(TEE_TDX_BITS, hexTag);
    }

    /**
     * Check if some bits are set on the TEE runtime framework range.
     *
     * @param expectedBits some bits expected to be in the tag
     * @param hexTag       tag of the deal
     * @return true if bits are set
     */
    static boolean hasTeeRuntimeFrameworkBitsInTag(final int expectedBits, final String hexTag) {
        return hexTag != null && Numeric.toBigInt(hexTag)
                .and(BigInteger.valueOf(TEE_RUNTIME_FRAMEWORK_MASK))
                .equals(BigInteger.valueOf(expectedBits));
    }

    /**
     * Returns TEE framework matching given {@code hexTag}.
     *
     * @param hexTag tag of the deal
     * @return {@link TeeFramework} matching given {@code hexTag}
     * or {@literal null} if tag is not a TEE tag or if there is no match.
     */
    public static TeeFramework getTeeFramework(String hexTag) {
        for (Map.Entry<Integer, TeeFramework> teeFramework : TEE_BITS_TO_FRAMEWORK.entrySet()) {
            if (hasTeeRuntimeFrameworkBitsInTag(teeFramework.getKey(), hexTag)) {
                return teeFramework.getValue();
            }
        }
        //TODO add TeeFramework.UNDEFINED
        return null;
    }

    /**
     * @deprecated not used
     */
    @Deprecated(forRemoval = true)
    public static boolean isTeeChallenge(String challenge) {
        return challenge != null && !challenge.equals(BytesUtils.EMPTY_ADDRESS);
    }
}
