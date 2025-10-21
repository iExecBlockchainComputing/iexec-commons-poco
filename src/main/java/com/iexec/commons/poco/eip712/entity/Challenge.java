/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.iexec.commons.poco.eip712.EIP712TypedData;
import com.iexec.commons.poco.eip712.EIP712Utils;
import com.iexec.commons.poco.utils.HashUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

/**
 * Represents the Challenge type in an EIP-712 compliant challenge.
 */
@Slf4j
@Value
@Builder
@JsonDeserialize(builder = Challenge.ChallengeBuilder.class)
public class Challenge implements EIP712TypedData {
    private static final String EIP712_TYPE = "Challenge(string challenge)";

    String challenge;

    public String computeMessageHash() {
        final String[] encodedValues = Stream.of(EIP712_TYPE, challenge)
                .map(EIP712Utils::encodeData)
                .toArray(String[]::new);
        if (log.isDebugEnabled()) {
            log.debug("{}", EIP712_TYPE);
            for (String value : encodedValues) {
                log.debug("{}", value);
            }
        }
        return HashUtils.concatenateAndHash(encodedValues);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ChallengeBuilder {
    }
}
