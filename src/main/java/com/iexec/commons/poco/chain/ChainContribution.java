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

import java.math.BigInteger;

import static com.iexec.commons.poco.chain.Web3jAbstractService.toBigInt;

@Slf4j
@Value
@Builder
public class ChainContribution {
    ChainContributionStatus status;
    String resultHash;
    String resultSeal;
    String enclaveChallenge;
    BigInteger weight;

    public static ChainContribution fromRawData(final String rawData) {
        log.debug("ChainContribution.fromRAwData");
        final String[] parts = PoCoDataDecoder.toParts(rawData);
        if (parts.length == 5) {
            return ChainContribution.builder()
                    .status(ChainContributionStatus.values()[toBigInt(parts[0]).intValue()])
                    .resultHash("0x" + parts[1])
                    .resultSeal("0x" + parts[2])
                    .enclaveChallenge("0x" + parts[3])
                    .weight(toBigInt(parts[4]))
                    .build();
        }
        return null;
    }
}
