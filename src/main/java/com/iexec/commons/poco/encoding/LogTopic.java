/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogTopic {

    public static final String TRANSFER_EVENT = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    public static final String REWARD_EVENT = "0xc2aca55aa696938c7e95842e8939ca0fbb2120a3eeb8948cdcee2b70da566672";
    public static final String SEIZE_EVENT = "0x1bccc549c38561cd5b57f0db11ceb8dde1b0b6ee05ab5e155b51c7c5ba64becb";
    public static final String LOCK_EVENT = "0x625fed9875dada8643f2418b838ae0bc78d9a148a18eee4ee1979ff0f3f5d427";
    public static final String UNLOCK_EVENT = "0x6381d9813cabeb57471b5a7e05078e64845ccdb563146a6911d536f24ce960f1";

    public static final String SCHEDULER_NOTICE_EVENT = "0x692ad61076dceddd0f1a861f737553dd61fc8501cf4190d29c4e90af6607f765";
    public static final String ORDERS_MATCHED_EVENT = "0xd811b592ed0899225773e8933d8df64bd0b62761a9d7aad4ed5b22735f4610a4";

    public static final String TASK_INITIALIZE_EVENT = "0x252992fb0468d68d6a5784ec03214f0d0a362083f2d7ebd157af43b017a22e06";
    public static final String TASK_CONTRIBUTE_EVENT = "0x3fdb8d7797562d49a81078dbf7fa1771958ea452f8b13d1148383bd9506aecfb";
    public static final String TASK_CONSENSUS_EVENT = "0xf6d49bf3e05d33a4bc497d3c793fb5756388bb96b947cf51bb60aaecb0e022e3";
    public static final String TASK_REVEAL_EVENT = "0x4b1763d473ac8fa80b4432ba90047e1b92444d8fabc55e6a002d9b1a316d7959";
    public static final String TASK_FINALIZE_EVENT = "0x78ce8a8bc0fcb704e8ba3b3dbb36aa88002df8038128b4af2f27ef65db665044";


    public static String decode(String topic) {
        return switch (topic) {
            case TRANSFER_EVENT -> "Transfer";
            case REWARD_EVENT -> "Reward";
            case SEIZE_EVENT -> "Seize";
            case LOCK_EVENT -> "Lock";
            case UNLOCK_EVENT -> "Unlock";
            case ORDERS_MATCHED_EVENT -> "OrdersMatched";
            case SCHEDULER_NOTICE_EVENT -> "SchedulerNotice";
            case TASK_INITIALIZE_EVENT -> "TaskInitialize";
            case TASK_CONTRIBUTE_EVENT -> "TaskContribute";
            case TASK_CONSENSUS_EVENT -> "TaskConsensus";
            case TASK_REVEAL_EVENT -> "TaskReveal";
            case TASK_FINALIZE_EVENT -> "TaskFinalize";
            default -> topic;
        };
    }

}
