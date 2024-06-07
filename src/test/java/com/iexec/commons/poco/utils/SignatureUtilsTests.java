/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.security.Signature;
import org.junit.jupiter.api.Test;

import static com.iexec.commons.poco.utils.SignatureUtils.isExpectedSignerOnSignedMessageHash;
import static com.iexec.commons.poco.utils.SignatureUtils.signMessageHashAndGetSignature;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SignatureUtilsTests {

    @Test
    void shouldMatchExpectedSigner() {
        final String messageHash = "0xf0cea2ffdb802c106aef2a032b01c7d271a454473709016c2e2c406097acdfd3";
        final String privateKey = "0x6dacd24b3d49d0c50c555aa728c60a57aa08beb363e3a90cce2e4e5d327c6ee2";
        final String address = CredentialsUtils.getAddress(privateKey);
        final Signature signature = signMessageHashAndGetSignature(messageHash, privateKey);

        final boolean isExpectedSigner = isExpectedSignerOnSignedMessageHash(messageHash, signature, address);

        assertThat(isExpectedSigner).isTrue();
    }

    @Test
    void isSignatureValid() {
        final String address = "0x043B9300356351419d9F63E818054E99e6c831b2";
        final String challenge = "0xe6fe0ad9cc7e30e83c9f9b6ce818ad20d576a496dd332cb1a257e85a7ebf2ca3";
        final String signature = "0x7e78fbf387024f96211345c9caae47bafccf9a992fbf7924b3473d133f73c02a32204b44309bb2eaba2fcfa3be726c2c1d19f4ca560ba0c82dbd18b724ea52a11b";
        assertThat(SignatureUtils.isSignatureValid(BytesUtils.stringToBytes(challenge), new Signature(signature), address.toLowerCase()))
                .isTrue();
        assertThat(SignatureUtils.isSignatureValid(BytesUtils.stringToBytes(challenge), new Signature(signature), address))
                .isTrue();
    }

}
