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

package com.iexec.commons.poco.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaitUtils {

    private WaitUtils() {
        throw new UnsupportedOperationException();
    }

    public static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            log.error("Failed to sleep [duration:{}, exception:{}]", seconds, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.error("Failed to sleepMs [duration:{}, exception:{}]", ms, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}
