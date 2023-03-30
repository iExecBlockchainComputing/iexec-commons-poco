/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.chain.eip712;

import com.iexec.commons.poco.utils.BytesUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

class EIP712UtilsTest {

    @Test
    void encodeUTF8String() {
        String someText = "some text";
        Assertions.assertThat(EIP712Utils.encodeData(someText))
                .isEqualTo(EIP712Utils.encodeUTF8String(someText))
                .isEqualTo("0x46ba1b442d3606a3437800ee7ae5a0249756405e676739b46aa8f6e85b13fe2b");
    }

    @Test
    void encodeHexString() {
        String param = "0x000000000000000000000000000000000000000000000000000000000000000a";
        Assertions.assertThat(EIP712Utils.encodeData(param))
                .isEqualTo(EIP712Utils.encodeHexString(param))
                .isEqualTo(param);
    }

    @Test
    void encodeHexStringIfLowerThanBytes32() {
        String param = "0x0a";
        Assertions.assertThat(EIP712Utils.encodeData(param))
                .isEqualTo(EIP712Utils.encodeHexString(param))
                .isEqualTo("0x000000000000000000000000000000000000000000000000000000000000000a");
    }

    @Test
    void shouldNotEncodeHexStringSinceNotHex() {
        String param = "xyz";
        Assertions.assertThat(EIP712Utils.encodeHexString(param)).isEmpty();
    }

    @Test
    void shouldNotEncodeHexStringSinceMoreThanBytes32() {
        String param = "0x000000000000000000000000000000000000000000000000000000000000000a" + "0b";
        Assertions.assertThat(EIP712Utils.encodeHexString(param)).isEmpty();
    }

    @Test
    void encodeLong() {
        Long param = 11L;
        Assertions.assertThat(EIP712Utils.encodeData(param))
                .isEqualTo(EIP712Utils.encodeLong(param))
                .isEqualTo("0x000000000000000000000000000000000000000000000000000000000000000b");
    }

    @Test
    void encodeBigInteger() {
        BigInteger param = BigInteger.valueOf(12);
        Assertions.assertThat(EIP712Utils.encodeData(param))
                .isEqualTo(EIP712Utils.encodeBigInteger(param))
                .isEqualTo("0x000000000000000000000000000000000000000000000000000000000000000c");
    }

    @Test
    void encodeByteArray() {
        String param = "0x000000000000000000000000000000000000000000000000000000000000000d";
        Assertions.assertThat(EIP712Utils.encodeData(BytesUtils.stringToBytes(param)))
                .isEqualTo(EIP712Utils.encodeByteArray(BytesUtils.stringToBytes(param)))
                .isEqualTo(param);
    }

    @Test
    void shouldNotEncodeByteArraySinceNotBytes32() {
        String param = "0x0a";
        Assertions.assertThat(EIP712Utils.encodeByteArray(BytesUtils.stringToBytes(param))).isEmpty();
    }
}