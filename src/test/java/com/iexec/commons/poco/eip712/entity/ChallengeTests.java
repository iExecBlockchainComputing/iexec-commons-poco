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

package com.iexec.commons.poco.eip712.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.utils.HashUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        final String payload = RandomStringUtils.randomAlphanumeric(20);
        final Challenge challenge = Challenge.builder()
                .challenge(payload)
                .build();
        String jsonString = mapper.writeValueAsString(challenge);
        assertThat(jsonString).isEqualTo("{\"challenge\":\"" + payload + "\"}");
        final Challenge parsedChallenge = mapper.readValue(jsonString, Challenge.class);
        assertThat(parsedChallenge).usingRecursiveComparison().isEqualTo(challenge);
        assertThat(challenge).hasToString("Challenge(challenge=" + payload + ")");
    }

    @Test
    void shouldComputeHashForChallenge() {
        final Challenge challenge = Challenge.builder()
                .challenge("challenge")
                .build();
        final EIP712Domain domain = new EIP712Domain("COMMON", "1", 15L, null);
        final String domainType = "EIP712Domain(string name,string version,uint256 chainId)";
        final String domainSeparator = HashUtils.concatenateAndHash(
                Numeric.toHexString(Hash.sha3(domainType.getBytes())),
                Numeric.toHexString(Hash.sha3(domain.getName().getBytes())),
                Numeric.toHexString(Hash.sha3(domain.getVersion().getBytes())),
                Numeric.toHexString(Numeric.toBytesPadded(BigInteger.valueOf(domain.getChainId()), 32)));
        final String messageType = "Challenge(string challenge)";
        final String messageHash = HashUtils.concatenateAndHash(
                Numeric.toHexString(Hash.sha3(messageType.getBytes())),
                Numeric.toHexString(Hash.sha3("challenge".getBytes())));
        assertThat(challenge.computeMessageHash()).isEqualTo(messageHash);
        final String hash = HashUtils.concatenateAndHash(
                "0x1901",
                domainSeparator,
                messageHash);
        assertThat(challenge.computeHash(domain))
                .isEqualTo(hash)
                .isEqualTo("0xea5ec041da81859f2c04a4876d5999ed8e66ad221b5b8699ca91f6814693a80e");
    }

}
