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
import lombok.*;
import org.web3j.tuples.generated.Tuple12;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class ChainTask {

    private ChainTaskStatus status;
    private String dealid;
    private int idx;
    private String chainTaskId;
    private long maxExecutionTime;
    private long contributionDeadline;
    private long revealDeadline;
    private long finalDeadline;
    private String consensusValue;
    private int revealCounter;
    private int winnerCounter;
    private List<String> contributors;
    private String results;


    public ChainTask(BigInteger status,
                     byte[] dealid,
                     BigInteger idx,
                     BigInteger maxExecutionTime,
                     BigInteger contributionDeadline,
                     BigInteger revealDeadline,
                     BigInteger finalDeadline,
                     byte[] consensusValue,
                     BigInteger revealCounter,
                     BigInteger winnerCounter,
                     List<String> contributors,
                     byte[] results) {
        this.setStatus(status);
        this.setDealid(dealid);
        this.setIdx(idx);
        this.setMaxExecutionTime(maxExecutionTime);
        this.setContributionDeadline(contributionDeadline);
        this.setRevealDeadline(revealDeadline);
        this.setFinalDeadline(finalDeadline);
        this.setConsensusValue(consensusValue);
        this.setRevealCounter(revealCounter);
        this.setWinnerCounter(winnerCounter);
        this.setContributors(contributors);
        this.setResults(results);

    }

    public static ChainTask tuple2ChainTask(Tuple12<BigInteger, byte[], BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, byte[], BigInteger, BigInteger, List<String>, byte[]> chainTask) {
        if (chainTask != null) {
            return new ChainTask(chainTask.component1(),
                    chainTask.component2(),
                    chainTask.component3(),
                    chainTask.component4(),
                    chainTask.component5(),
                    chainTask.component6(),
                    chainTask.component7(),
                    chainTask.component8(),
                    chainTask.component9(),
                    chainTask.component10(),
                    chainTask.component11(),
                    chainTask.component12());
        }
        return null;
    }

    private void setStatus(BigInteger status) {
        this.status = ChainTaskStatus.getValue(status);
    }

    private void setDealid(byte[] dealid) {
        this.dealid = BytesUtils.bytesToString(dealid);
    }

    private void setIdx(BigInteger idx) {
        this.idx = idx.intValue();
    }

    private void setMaxExecutionTime(BigInteger maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime.longValue() * 1000L;
    }

    private void setContributionDeadline(BigInteger contributionDeadline) {
        this.contributionDeadline = contributionDeadline.longValue() * 1000L;
    }

    private void setRevealDeadline(BigInteger revealDeadline) {
        this.revealDeadline = revealDeadline.longValue() * 1000L;
    }

    private void setFinalDeadline(BigInteger finalDeadline) {
        this.finalDeadline = finalDeadline.longValue() * 1000L;
    }

    private void setConsensusValue(byte[] consensusValue) {
        this.consensusValue = BytesUtils.bytesToString(consensusValue);
    }

    private void setRevealCounter(BigInteger revealCounter) {
        this.revealCounter = revealCounter.intValue();
    }

    private void setWinnerCounter(BigInteger winnerCounter) {
        this.winnerCounter = winnerCounter.intValue();
    }

    public void setContributors(List<String> contributors) {
        this.contributors = contributors;
    }

    private void setResults(byte[] results) {
        this.results = BytesUtils.bytesToString(results);
    }

    public String getChainTaskId() {
        return ChainUtils.generateChainTaskId(dealid, idx);
    }
}
