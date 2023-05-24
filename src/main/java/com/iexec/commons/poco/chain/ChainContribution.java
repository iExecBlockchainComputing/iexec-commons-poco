/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.utils.BytesUtils;
import lombok.Builder;
import lombok.Value;
import org.web3j.tuples.generated.Tuple4;

import java.math.BigInteger;

@Value
@Builder
public class ChainContribution {

    ChainContributionStatus status;
    String resultHash;
    String resultSeal;
    String enclaveChallenge;

    public static ChainContribution tuple2Contribution(Tuple4<BigInteger, byte[], byte[], String> contribution) {
        if (contribution != null) {
            return ChainContribution.builder()
                    .status(ChainContributionStatus.getValue(contribution.component1()))
                    .resultHash(BytesUtils.bytesToString(contribution.component2()))
                    .resultSeal(BytesUtils.bytesToString(contribution.component3()))
                    .enclaveChallenge(contribution.component4())
                    .build();
        }
        return null;
    }

}
