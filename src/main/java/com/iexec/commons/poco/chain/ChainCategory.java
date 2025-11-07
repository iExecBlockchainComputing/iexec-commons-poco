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

package com.iexec.commons.poco.chain;

import com.iexec.commons.poco.encoding.PoCoDataDecoder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import static com.iexec.commons.poco.chain.Web3jAbstractService.toBigInt;

@Slf4j
@Value
@Builder
public class ChainCategory {
    long id;
    String name;
    String description;
    long maxExecutionTime;

    public static ChainCategory fromRawData(final long id, final String rawData) {
        log.debug("ChainCategory.fromRawData");
        final String[] parts = PoCoDataDecoder.toParts(rawData);
        int offset = toBigInt(parts[0]).intValue() / 32;
        final int nameContribOffset = toBigInt(parts[offset]).intValue() / 32;
        final int descriptionContribOffset = toBigInt(parts[offset + 1]).intValue() / 32;
        return ChainCategory.builder()
                .id(id)
                .name(PoCoDataDecoder.decodeToAsciiString(parts, offset + nameContribOffset))
                .description(PoCoDataDecoder.decodeToAsciiString(parts, offset + descriptionContribOffset))
                .maxExecutionTime(toBigInt(parts[offset + 2]).longValue() * 1000)
                .build();
    }
}
