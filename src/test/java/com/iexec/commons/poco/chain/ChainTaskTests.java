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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChainTaskTests {

    @Test
    void shouldHaveContributions() {
        final ChainTask chainTask = ChainTask.builder()
                .contributors(List.of("0x1"))
                .build();
        assertThat(chainTask.hasContributions()).isTrue();
    }

    @Test
    void shouldNotHaveContributions() {
        final ChainTask chainTask = ChainTask.builder().build();
        assertThat(chainTask.hasContributions()).isFalse();
    }

    @Test
    void shouldHaveContributionFrom() {
        final ChainTask chainTask = ChainTask.builder()
                .contributors(List.of("0x1"))
                .build();
        assertThat(chainTask.hasContributionFrom("0x1")).isTrue();
    }

    @Test
    void shouldNotHaveContributionFrom() {
        final ChainTask chainTask = ChainTask.builder()
                .contributors(List.of("0x1"))
                .build();
        assertThat(chainTask.hasContributionFrom("0x2")).isFalse();
    }

    @Test
    void shouldBeAfterDeadlines() {
        final long deadline = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
        final ChainTask chainTask = ChainTask.builder()
                .contributionDeadline(deadline)
                .revealDeadline(deadline)
                .finalDeadline(deadline)
                .build();
        assertThat(chainTask.isContributionDeadlineReached()).isTrue();
        assertThat(chainTask.isRevealDeadlineReached()).isTrue();
        assertThat(chainTask.isFinalDeadlineReached()).isTrue();
    }

    @Test
    void shouldBeBeforeDeadlines() {
        final long deadline = Instant.now().plus(1, ChronoUnit.MINUTES).toEpochMilli();
        final ChainTask chainTask = ChainTask.builder()
                .contributionDeadline(deadline)
                .revealDeadline(deadline)
                .finalDeadline(deadline)
                .build();
        assertThat(chainTask.isContributionDeadlineReached()).isFalse();
        assertThat(chainTask.isRevealDeadlineReached()).isFalse();
        assertThat(chainTask.isFinalDeadlineReached()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideArgsForIsRevealed")
    void shouldBeRevealed(final int winnerCounter, final int revealCounter, final Instant revealDeadline) {
        final ChainTask chainTask = ChainTask.builder()
                .winnerCounter(winnerCounter)
                .revealCounter(revealCounter)
                .revealDeadline(revealDeadline.toEpochMilli())
                .build();
        assertThat(chainTask.isRevealed()).isTrue();
    }

    private static Stream<Arguments> provideArgsForIsRevealed() {
        return Stream.of(
                Arguments.of(1, 1, Instant.now().plus(1, ChronoUnit.MINUTES)),
                Arguments.of(1, 1, Instant.now().minus(1, ChronoUnit.MINUTES)),
                Arguments.of(2, 1, Instant.now().minus(1, ChronoUnit.MINUTES))
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgsForIsNotRevealed")
    void shouldNotBeRevealed(final int winnerCounter, final int revealCounter, final Instant revealDeadline) {
        final ChainTask chainTask = ChainTask.builder()
                .winnerCounter(winnerCounter)
                .revealCounter(revealCounter)
                .revealDeadline(revealDeadline.toEpochMilli())
                .build();
        assertThat(chainTask.isRevealed()).isFalse();
    }


    private static Stream<Arguments> provideArgsForIsNotRevealed() {
        return Stream.of(
                Arguments.of(1, 0, Instant.now().plus(1, ChronoUnit.MINUTES)),
                Arguments.of(2, 1, Instant.now().plus(1, ChronoUnit.MINUTES)),
                Arguments.of(0, 0, Instant.now().minus(1, ChronoUnit.MINUTES))
        );
    }
}
