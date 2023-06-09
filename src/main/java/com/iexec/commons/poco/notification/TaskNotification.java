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

package com.iexec.commons.poco.notification;

import com.iexec.commons.poco.task.TaskAbortCause;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskNotification {

    // Id of the task concerned by the notification.
    String chainTaskId;

    // List of workers targeted by the notification.
    List<String> workersAddress;

    // Type of the notification.
    TaskNotificationType taskNotificationType;

    // Optional extra metadata provided with the notification    
    TaskNotificationExtra taskNotificationExtra;

    /**
     * Get the abort cause of this task. If the cause is not defined by
     * the notification sender, {@code TaskAbortCause#UNKNOWN} is returned.
     * 
     * @return
     */
    public TaskAbortCause getTaskAbortCause() {
        return taskNotificationExtra != null && taskNotificationExtra.getTaskAbortCause() != null
                ? taskNotificationExtra.getTaskAbortCause()
                : TaskAbortCause.UNKNOWN;
    }
}
