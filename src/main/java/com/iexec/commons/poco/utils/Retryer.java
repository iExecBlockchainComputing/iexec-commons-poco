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

package com.iexec.commons.poco.utils;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
public class Retryer<T> {

    /**
     * Executes and retries with a delay a supplying method until a predicate is
     * true.
     *
     * @param supplier         supplying method to be executed
     * @param retryIfPredicate condition for retrying the supplying method call
     * @param retryDelay       delay in ms between two tries
     * @param maxRetry         number of maximum retries
     * @param logContext       human-readable content to be displayed
     * @return an object that the supplying method provides
     */
    public T repeatCall(final CheckedSupplier<T> supplier,
                        final Predicate<T> retryIfPredicate,
                        final long retryDelay,
                        final int maxRetry,
                        final String logContext) {
        if (retryDelay == 0) {
            log.error("Cannot repeat call {} without delay [retryDelay:{}ms, maxRetry:{}]",
                    logContext, retryDelay, maxRetry);
            return null;
        }
        final RetryPolicy<T> retryPolicy =
                new RetryPolicy<T>()
                        .handleResultIf(retryIfPredicate) //retry if
                        .withDelay(Duration.ofMillis(retryDelay))
                        .withMaxRetries(maxRetry)
                        .onRetry(e -> logWarnRetry(
                                logContext, retryDelay, maxRetry, e.getAttemptCount()))
                        .onRetriesExceeded(e -> logErrorOnMaxRetry(
                                logContext, retryDelay, maxRetry));
        return Failsafe.with(retryPolicy)
                .get(supplier);
    }

    private void logWarnRetry(final String context, final long retryDelay, final int maxRetry, final int attempt) {
        log.warn("Failed to \"{}\", about to retry [retryDelay:{}ms, maxRetry:{}, attempt:{}]",
                context, retryDelay, maxRetry, attempt);
    }

    private void logErrorOnMaxRetry(final String context, final long retryDelay, final int maxRetry) {
        log.error("Failed to \"{}\" after max retry [retryDelay:{}ms, maxRetry:{}]",
                context, retryDelay, maxRetry);
    }

}