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

package com.iexec.commons.poco.task;

import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.dapp.DappType;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.commons.poco.tee.TeeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.iexec.commons.poco.utils.BytesUtils.EMPTY_ADDRESS;
import static org.junit.jupiter.api.Assertions.*;

class TaskDescriptionTests {

    private static final String CHAIN_TASK_ID = "chainTaskId";

    private static final String APP_ADDRESS = "appAddress";
    private static final String APP_OWNER = "0x1";
    private static final BigInteger APP_PRICE = BigInteger.ZERO;
    private static final String DATASET_ADDRESS = "datasetAddress";
    private static final String DATASET_OWNER = "0x2";
    private static final BigInteger DATASET_PRICE = BigInteger.ONE;
    private static final String WORKERPOOL_ADDRESS = "";
    private static final String WORKERPOOL_OWNER = "0x3";
    private static final BigInteger WORKERPOOL_PRICE = BigInteger.TEN;
    private static final BigInteger TRUST = BigInteger.ONE;
    private static final BigInteger CATEGORY = BigInteger.ZERO;

    private static final String REQUESTER = "requester";
    private static final String BENEFICIARY = "beneficiary";
    private static final String CALLBACK = "callback";
    private static final DappType APP_TYPE = DappType.DOCKER;
    private static final String APP_URI = "https://uri";
    private static final String ENTRYPOINT = "entrypoint";
    private static final String CMD = "cmd";
    private static final int MAX_EXECUTION_TIME = 1;
    private static final boolean IS_TEE_TASK = true;
    private static final TeeFramework TEE_FRAMEWORK = TeeFramework.SCONE;
    private static final long START_TIME = 1_000_000L;
    private static final int BOT_SIZE = 1;
    private static final int BOT_FIRST = 2;
    private static final int TASK_IDX = 3;
    private static final String DATASET_URI = "https://datasetUri";
    private static final String DATASET_CHECKSUM = "datasetChecksum";
    private static final List<String> INPUT_FILES = Collections.singletonList("inputFiles");
    private static final boolean IS_RESULT_ENCRYPTION = true;
    private static final String RESULT_STORAGE_PROVIDER = "resultStorageProvider";
    private static final String RESULT_STORAGE_PROXY = "resultStorageProxy";

    @Test
    void toTaskDescriptionWithNullDeal() {
        assertNull(TaskDescription.toTaskDescription(null, null));
        assertNull(TaskDescription.toTaskDescription(ChainDeal.builder().build(), null));
        assertNull(TaskDescription.toTaskDescription(null, ChainTask.builder().build()));
    }

    @Test
    void toTaskDescription() {
        final ChainCategory chainCategory = ChainCategory.builder()
                .maxExecutionTime(MAX_EXECUTION_TIME)
                .build();
        final TeeEnclaveConfiguration enclaveConfiguration = TeeEnclaveConfiguration.builder()
                .entrypoint(ENTRYPOINT)
                .build();
        final ChainApp chainApp = ChainApp.builder()
                .chainAppId(APP_ADDRESS)
                .type(APP_TYPE.toString())
                .multiaddr(APP_URI)
                .enclaveConfiguration(enclaveConfiguration)
                .build();
        final ChainDataset chainDataset = ChainDataset.builder()
                .chainDatasetId(DATASET_ADDRESS)
                .multiaddr(DATASET_URI)
                .checksum(DATASET_CHECKSUM)
                .build();
        final DealParams dealParams = DealParams.builder()
                .iexecArgs(CMD)
                .iexecInputFiles(INPUT_FILES)
                .iexecResultStorageProvider(RESULT_STORAGE_PROVIDER)
                .iexecResultStorageProxy(RESULT_STORAGE_PROXY)
                .iexecResultEncryption(IS_RESULT_ENCRYPTION)
                .build();
        final ChainDeal chainDeal = ChainDeal.builder()
                .chainApp(chainApp)
                .chainDataset(chainDataset)
                .chainCategory(chainCategory)
                .dappPointer(APP_ADDRESS)
                .dappOwner(APP_OWNER)
                .dappPrice(APP_PRICE)
                .dataPointer(DATASET_ADDRESS)
                .dataOwner(DATASET_OWNER)
                .dataPrice(DATASET_PRICE)
                .poolPointer(WORKERPOOL_ADDRESS)
                .poolOwner(WORKERPOOL_OWNER)
                .poolPrice(WORKERPOOL_PRICE)
                .trust(TRUST)
                .category(CATEGORY)
                .tag(TeeUtils.TEE_SCONE_ONLY_TAG) // any supported TEE tag
                .requester(REQUESTER)
                .beneficiary(BENEFICIARY)
                .callback(CALLBACK)
                .params(dealParams)
                .startTime(BigInteger.valueOf(START_TIME))
                .botFirst(BigInteger.valueOf(BOT_FIRST))
                .botSize(BigInteger.valueOf(BOT_SIZE))
                .build();
        final ChainTask chainTask = ChainTask.builder()
                .dealid(chainDeal.getChainDealId())
                .chainTaskId(CHAIN_TASK_ID)
                .idx(TASK_IDX)
                .build();

        final TaskDescription task = TaskDescription.toTaskDescription(chainDeal, chainTask);

        final TaskDescription expectedTaskDescription = TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                // assets
                .appType(APP_TYPE)
                .appUri(APP_URI)
                .appEnclaveConfiguration(enclaveConfiguration)
                // deals
                .appAddress(APP_ADDRESS)
                .appOwner(APP_OWNER)
                .appPrice(APP_PRICE)
                .datasetAddress(DATASET_ADDRESS)
                .datasetOwner(DATASET_OWNER)
                .datasetPrice(DATASET_PRICE)
                .workerpoolAddress(WORKERPOOL_ADDRESS)
                .workerpoolOwner(WORKERPOOL_OWNER)
                .workerpoolPrice(WORKERPOOL_PRICE)
                .trust(TRUST)
                .category(CATEGORY)
                .isTeeTask(IS_TEE_TASK)
                .teeFramework(TEE_FRAMEWORK)
                .requester(REQUESTER)
                .beneficiary(BENEFICIARY)
                .callback(CALLBACK)
                .dealParams(dealParams)
                .startTime(START_TIME)
                .botFirstIndex(BOT_FIRST)
                .botSize(BOT_SIZE)
                // task
                .maxExecutionTime(MAX_EXECUTION_TIME)
                .botIndex(TASK_IDX)
                .datasetUri(DATASET_URI)
                .datasetChecksum(DATASET_CHECKSUM)
                .build();

        assertEquals(expectedTaskDescription, task);
        assertTrue(task.containsCallback());
    }

    // region containsDataset
    @Test
    void shouldContainDataset() {
        assertTrue(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertTrue(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());
    }

    @Test
    void shouldNotContainDataset() {
        assertFalse(TaskDescription.builder()
                // .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertFalse(TaskDescription.builder()
                .datasetAddress(EMPTY_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertFalse(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                // .datasetUri(DATASET_URI)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertFalse(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                // .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());
    }
    // endregion

    // region containsCallback
    @Test
    void shouldContainCallback() {
        assertTrue(TaskDescription.builder()
                .callback(CALLBACK)
                .build()
                .containsCallback());
    }

    @Test
    void shouldNotContainCallback() {
        assertFalse(TaskDescription.builder()
                .callback(EMPTY_ADDRESS)
                .build()
                .containsCallback());
        assertFalse(TaskDescription.builder()
                // .callback(CALLBACK)
                .build()
                .containsCallback());
    }
    // endregion

    // region containsInputFiles
    @Test
    void shouldContainsInputFiles() {
        assertTrue(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .dealParams(DealParams.builder().iexecInputFiles(List.of("http://file1", "http://file2")).build())
                .build()
                .containsInputFiles());
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("provideDealParamsWithoutInputFiles")
    void shouldNotContainInputFiles(final DealParams dealParams) {
        assertFalse(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .dealParams(dealParams)
                .build()
                .containsInputFiles());
    }

    private static Stream<Arguments> provideDealParamsWithoutInputFiles() {
        return Stream.of(
                Arguments.of(DealParams.builder().build()),
                Arguments.of(DealParams.builder().iexecInputFiles(null).build()),
                Arguments.of(DealParams.builder().iexecInputFiles(List.of()).build())
        );
    }
    // endregion

    // region getAppCommand
    @ParameterizedTest
    @NullSource
    @MethodSource("provideDealParamsWithoutArgs")
    void shouldGenerateAppCommandWithEntrypointOnly(final DealParams dealParams) {
        final TaskDescription taskDescription = TaskDescription.builder()
                .appEnclaveConfiguration(TeeEnclaveConfiguration.builder().entrypoint(ENTRYPOINT).build())
                .dealParams(dealParams)
                .build();
        assertEquals(ENTRYPOINT, taskDescription.getAppCommand());
    }

    private static Stream<Arguments> provideDealParamsWithoutArgs() {
        return Stream.of(
                Arguments.of(DealParams.builder().build()),
                Arguments.of(DealParams.builder().iexecArgs(null).build()),
                Arguments.of(DealParams.builder().iexecArgs("").build()),
                Arguments.of(DealParams.builder().iexecArgs(" ").build())
        );
    }

    @Test
    void shouldGenerateAppCommandWithEntrypointAndArgs() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .appEnclaveConfiguration(TeeEnclaveConfiguration.builder().entrypoint(ENTRYPOINT).build())
                .dealParams(DealParams.builder().iexecArgs(CMD).build())
                .build();
        assertEquals(ENTRYPOINT + " " + CMD, taskDescription.getAppCommand());
    }
    // endregion

    // region isEligibleToContributeAndFinalize
    @ParameterizedTest
    @ValueSource(strings = {"", CALLBACK})
    void shouldBeEligibleToContributeAndFinalize(final String callback) {
        final TaskDescription taskDescription = TaskDescription.builder()
                .isTeeTask(true)
                .trust(BigInteger.ONE)
                .callback(callback)
                .build();

        assertTrue(taskDescription.isEligibleToContributeAndFinalize());
    }

    @Test
    void shouldNotBeEligibleToContributeAndFinalizeSinceNotTee() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .isTeeTask(false)
                .trust(BigInteger.ONE)
                .callback("")
                .build();

        assertFalse(taskDescription.isEligibleToContributeAndFinalize());
    }

    @Test
    void shouldNotBeEligibleToContributeAndFinalizeSinceWrongTrust() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .isTeeTask(true)
                .trust(BigInteger.TEN)
                .callback("")
                .build();

        assertFalse(taskDescription.isEligibleToContributeAndFinalize());
    }
    // endregion
}
