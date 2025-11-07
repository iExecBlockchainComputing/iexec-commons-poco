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

package com.iexec.commons.poco.encoding;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;

import static com.iexec.commons.poco.encoding.LogTopic.*;
import static org.assertj.core.api.Assertions.assertThat;

class LogTopicTests {
    private String getTopic(final String eventSignature) {
        return Hash.sha3String(eventSignature);
    }

    @Test
    void checkTopicValues() {
        assertThat(getTopic("SchedulerNotice(address,bytes32)")).isEqualTo(SCHEDULER_NOTICE_EVENT);
        assertThat(getTopic("TaskInitialize(bytes32,address)")).isEqualTo(TASK_INITIALIZE_EVENT);
        assertThat(getTopic("TaskContribute(bytes32,address,bytes32)")).isEqualTo(TASK_CONTRIBUTE_EVENT);
        assertThat(getTopic("TaskConsensus(bytes32,bytes32)")).isEqualTo(TASK_CONSENSUS_EVENT);
        assertThat(getTopic("TaskReveal(bytes32,address,bytes32)")).isEqualTo(TASK_REVEAL_EVENT);
        assertThat(getTopic("TaskFinalize(bytes32,bytes)")).isEqualTo(TASK_FINALIZE_EVENT);
    }

    @Test
    void checkDecode() {
        assertThat(LogTopic.decode(SCHEDULER_NOTICE_EVENT)).isEqualTo("SchedulerNotice");
        assertThat(LogTopic.decode(TASK_INITIALIZE_EVENT)).isEqualTo("TaskInitialize");
        assertThat(LogTopic.decode(TASK_CONTRIBUTE_EVENT)).isEqualTo("TaskContribute");
        assertThat(LogTopic.decode(TASK_CONSENSUS_EVENT)).isEqualTo("TaskConsensus");
        assertThat(LogTopic.decode(TASK_REVEAL_EVENT)).isEqualTo("TaskReveal");
        assertThat(LogTopic.decode(TASK_FINALIZE_EVENT)).isEqualTo("TaskFinalize");
    }
}
