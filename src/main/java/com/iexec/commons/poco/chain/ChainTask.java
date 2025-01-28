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

import com.iexec.commons.poco.utils.BytesUtils;
import lombok.Builder;
import lombok.Value;
import org.web3j.tuples.generated.Tuple12;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ChainTask {

    ChainTaskStatus status;
    String dealid;
    int idx;
    String chainTaskId;
    long maxExecutionTime;
    long contributionDeadline;
    long revealDeadline;
    long finalDeadline;
    String consensusValue;
    int revealCounter;
    int winnerCounter;
    List<String> contributors;
    String results;

    public boolean hasContributor(final String address) {
        return contributors.contains(address);
    }

    public boolean isContributionDeadlineReached() {
        return contributionDeadline <= Instant.now().toEpochMilli();
    }

    public boolean isRevealDeadlineReached() {
        return revealDeadline <= Instant.now().toEpochMilli();
    }

    public boolean isRevealed() {
        return revealCounter > 0 && (revealCounter == winnerCounter || isRevealDeadlineReached());
    }

    public boolean isFinalDeadlineReached() {
        return finalDeadline <= Instant.now().toEpochMilli();
    }

    public static ChainTask tuple2ChainTask(Tuple12<BigInteger, byte[], BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, byte[], BigInteger, BigInteger, List<String>, byte[]> chainTask) {
        if (chainTask != null) {
            return ChainTask.builder()
                    .status(ChainTaskStatus.getValue(chainTask.component1()))
                    .dealid(BytesUtils.bytesToString(chainTask.component2()))
                    .idx(chainTask.component3().intValue())
                    .maxExecutionTime(chainTask.component4().longValue() * 1000L)
                    .contributionDeadline(chainTask.component5().longValue() * 1000L)
                    .revealDeadline(chainTask.component6().longValue() * 1000L)
                    .finalDeadline(chainTask.component7().longValue() * 1000L)
                    .consensusValue(BytesUtils.bytesToString(chainTask.component8()))
                    .revealCounter(chainTask.component9().intValue())
                    .winnerCounter(chainTask.component10().intValue())
                    .contributors(chainTask.component11())
                    .results(BytesUtils.bytesToString(chainTask.component12()))
                    .chainTaskId(ChainUtils.generateChainTaskId(BytesUtils.bytesToString(chainTask.component2()), chainTask.component3().intValue()))
                    .build();
        }
        return null;
    }

}
