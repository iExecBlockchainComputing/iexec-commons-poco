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

package com.iexec.commons.poco.utils;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashUtilsTests {

    @Test
    void shouldBeCorrectOneValue() {
        String hexa1 = "0x748e091bf16048cb5103E0E10F9D5a8b7fBDd860";

        String expected = Hash.sha3(hexa1);

        assertEquals(expected, HashUtils.concatenateAndHash(hexa1));
    }

    @Test
    void shouldBeCorrectTwoValues() {
        String hexa1 = "0x748e091bf16048cb5103E0E10F9D5a8b7fBDd860";
        String hexa2 = "0xd94b63fc2d3ec4b96daf84b403bbafdc8c8517e8e2addd51fec0fa4e67801be8";

        String expected = "0x9ca8cbf81a285c62778678c874dae13fdc6857566b67a9a825434dd557e18a8d";

        assertEquals(expected, HashUtils.concatenateAndHash(hexa1, hexa2));
    }

    @Test
    void shouldBeCorrectThreeValues() {
        String hexa1 = "0x748e091bf16048cb5103E0E10F9D5a8b7fBDd860";
        String hexa2 = "0xd94b63fc2d3ec4b96daf84b403bbafdc8c8517e8e2addd51fec0fa4e67801be8";
        String hexa3 = "0x9a43BB008b7A657e1936ebf5d8e28e5c5E021596";

        String expected = "0x54a76d209e8167e1ffa3bde8e3e7b30068423ca9554e1d605d8ee8fd0f165562";

        assertEquals(expected, HashUtils.concatenateAndHash(hexa1, hexa2, hexa3));
    }

    @Test
    void shouldGetStringSha256() {
        assertEquals("0xb33845db05fb0822f1f1e3677cc6787b8a1a7a21f3c12f9e97c70cb596222218",
                HashUtils.sha256("utf8String"));
    }

    @Test
    void shouldGetBytesSha256() {
        byte[] bytes = "hello".getBytes();
        assertEquals("0x2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                HashUtils.sha256(bytes));
    }

}
