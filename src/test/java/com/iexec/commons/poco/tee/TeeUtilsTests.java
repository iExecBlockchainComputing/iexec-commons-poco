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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static com.iexec.commons.poco.utils.BytesUtils.toByte32HexString;
import static org.assertj.core.api.Assertions.assertThat;

class TeeUtilsTests {

    @Test
    void areValidFields() {
        assertThat(TeeUtils.TEE_SCONE_BITS).isEqualTo(0b0011);
        assertThat(TeeUtils.TEE_GRAMINE_BITS).isEqualTo(0b0101);
        assertThat(TeeUtils.TEE_TDX_BITS).isEqualTo(0b1001);
        assertThat(TeeUtils.TEE_SCONE_ONLY_TAG)
                .isEqualTo("0x0000000000000000000000000000000000000000000000000000000000000003");
        assertThat(TeeUtils.TEE_GRAMINE_ONLY_TAG)
                .isEqualTo("0x0000000000000000000000000000000000000000000000000000000000000005");
        assertThat(TeeUtils.TEE_TDX_ONLY_TAG)
                .isEqualTo("0x0000000000000000000000000000000000000000000000000000000000000009");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x3, 0x5, 0xf3, 0xf5})
    void isTeeTag(int tag) {
        assertThat(TeeUtils.isTeeTag(toByte32HexString(tag))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            0b0000, 0b0001, 0b0010,
            0b0100, 0b0110, 0b0111,
            0b1000, 0b1001, 0b1010, 0b1011,
            0b1100, 0b1101, 0b1110, 0b1111
    })
    void isNotTeeTag(int tag) {
        assertThat(TeeUtils.isTeeTag(toByte32HexString(tag))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x3, 0xf3})
    void hasTeeSconeInTag(final int tag) {
        assertThat(TeeUtils.hasTeeSconeInTag(toByte32HexString(tag))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x5, 0x9, 0xf5, 0xf9})
    void hasNotTeeSconeInTag(final int tag) {
        assertThat(TeeUtils.hasTeeSconeInTag(toByte32HexString(tag))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x5, 0xf5})
    void hasTeeGramineInTag(final int tag) {
        assertThat(TeeUtils.hasTeeGramineInTag(toByte32HexString(tag))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x3, 0x9, 0xf3, 0xf9})
    void hasNotTeeGramineInTag(final int tag) {
        assertThat(TeeUtils.hasTeeGramineInTag(toByte32HexString(tag))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x9, 0xf9})
    void hasTeeTdxInTag(final int tag) {
        assertThat(TeeUtils.hasTeeTdxInTag(toByte32HexString(tag))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x3, 0x5, 0xf3, 0xf5})
    void hasNotTeeTdxInTag(final int tag) {
        assertThat(TeeUtils.hasTeeTdxInTag(toByte32HexString(tag))).isFalse();
    }

    // ensures some bits are present within TEE runtime framework mask
    static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(0b0001, 0b0001),       // exact match
                Arguments.of(0b0001, 0b11110001),   // contains
                // ...
                Arguments.of(0b1111, 0b1111),       // exact match
                Arguments.of(0b1111, 0b11111111)    // contains
        );
    }

    @ParameterizedTest
    @MethodSource("validData")
    void hasTeeRuntimeFrameworkBitsInTag(final int expectedBits, final int tag) {
        assertThat(TeeUtils.hasTeeRuntimeFrameworkBitsInTag(expectedBits, toByte32HexString(tag))).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "abc", // after mask, 0xc -> 0b1100 does not match expected 0b1010
            "0x1", // no match
    })
    void hasNotTeeRuntimeFrameworkBitsInTag(String tag) {
        assertThat(TeeUtils.hasTeeRuntimeFrameworkBitsInTag(0b1010, tag)).isFalse();
    }
}
