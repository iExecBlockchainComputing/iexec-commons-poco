/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.utils.BytesUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.iexec.commons.poco.utils.BytesUtils.EMPTY_ADDRESS;
import static org.junit.jupiter.api.Assertions.*;

class TaskDescriptionTests {

    private static final String APP_OWNER = "0x1";
    private static final BigInteger APP_PRICE = BigInteger.ZERO;
    private static final String DATA_OWNER = "0x2";
    private static final BigInteger DATA_PRICE = BigInteger.ONE;
    private static final String WORKERPOOL_OWNER = "0x3";
    private static final BigInteger WORKERPOOL_PRICE = BigInteger.TEN;

    private static final String CHAIN_TASK_ID = "chainTaskId";
    private static final String REQUESTER = "requester";
    private static final String BENEFICIARY = "beneficiary";
    private static final String CALLBACK = "callback";
    private static final DappType APP_TYPE = DappType.DOCKER;
    private static final String APP_URI = "https://uri";
    private static final String APP_ADDRESS = "appAddress";
    private static final String ENTRYPOINT = "entrypoint";
    private static final String CMD = "cmd";
    private static final int MAX_EXECUTION_TIME = 1;
    private static final boolean IS_TEE_TASK = true;
    private static final TeeFramework TEE_FRAMEWORK = TeeFramework.SCONE;
    private static final int BOT_SIZE = 1;
    private static final int BOT_FIRST = 2;
    private static final int TASK_IDX = 3;
    private static final String DATASET_ADDRESS = "datasetAddress";
    private static final String DATASET_URI = "https://datasetUri";
    private static final String DATASET_NAME = "datasetName";
    private static final String DATASET_CHECKSUM = "datasetChecksum";
    private static final List<String> INPUT_FILES = Collections.singletonList("inputFiles");
    private static final boolean IS_RESULT_ENCRYPTION = true;
    private static final String RESULT_STORAGE_PROVIDER = "resultStorageProvider";
    private static final String RESULT_STORAGE_PROXY = "resultStorageProxy";
    private static final BigInteger TRUST = BigInteger.ONE;

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
                .uri(BytesUtils.bytesToString(APP_URI.getBytes(StandardCharsets.UTF_8)))
                .enclaveConfiguration(enclaveConfiguration)
                .build();
        final ChainDataset chainDataset = ChainDataset.builder()
                .chainDatasetId(DATASET_ADDRESS)
                .name(DATASET_NAME)
                .uri(BytesUtils.bytesToString(DATASET_URI.getBytes(StandardCharsets.UTF_8)))
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
                .dappOwner(APP_OWNER)
                .dappPrice(APP_PRICE)
                .dataOwner(DATA_OWNER)
                .dataPrice(DATA_PRICE)
                .poolOwner(WORKERPOOL_OWNER)
                .poolPrice(WORKERPOOL_PRICE)
                .requester(REQUESTER)
                .beneficiary(BENEFICIARY)
                .callback(CALLBACK)
                .chainApp(chainApp)
                .params(dealParams)
                .chainDataset(chainDataset)
                .tag(TeeUtils.TEE_SCONE_ONLY_TAG) // any supported TEE tag
                .chainCategory(chainCategory)
                .botFirst(BigInteger.valueOf(BOT_FIRST))
                .botSize(BigInteger.valueOf(BOT_SIZE))
                .trust(TRUST)
                .build();
        final ChainTask chainTask = ChainTask.builder()
                .dealid(chainDeal.getChainDealId())
                .chainTaskId(CHAIN_TASK_ID)
                .idx(TASK_IDX)
                .build();

        final TaskDescription task = TaskDescription.toTaskDescription(chainDeal, chainTask);

        final TaskDescription expectedTaskDescription = TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .appOwner(APP_OWNER)
                .appPrice(APP_PRICE)
                .datasetOwner(DATA_OWNER)
                .datasetPrice(DATA_PRICE)
                .workerpoolOwner(WORKERPOOL_OWNER)
                .workerpoolPrice(WORKERPOOL_PRICE)
                .requester(REQUESTER)
                .beneficiary(BENEFICIARY)
                .callback(CALLBACK)
                .appType(APP_TYPE)
                .appUri(APP_URI)
                .appAddress(APP_ADDRESS)
                .appEnclaveConfiguration(enclaveConfiguration)
                .maxExecutionTime(MAX_EXECUTION_TIME)
                .isTeeTask(IS_TEE_TASK)
                .teeFramework(TEE_FRAMEWORK)
                .botSize(BOT_SIZE)
                .botFirstIndex(BOT_FIRST)
                .botIndex(TASK_IDX)
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .cmd(CMD)
                .inputFiles(INPUT_FILES)
                .isResultEncryption(IS_RESULT_ENCRYPTION)
                .resultStorageProvider(RESULT_STORAGE_PROVIDER)
                .resultStorageProxy(RESULT_STORAGE_PROXY)
                .secrets(Collections.emptyMap())
                .dealParams(dealParams)
                .trust(TRUST)
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
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertTrue(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                // .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());
    }

    @Test
    void shouldNotContainDataset() {
        assertFalse(TaskDescription.builder()
                // .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertFalse(TaskDescription.builder()
                .datasetAddress(EMPTY_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertFalse(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                // .datasetUri(DATASET_URI)
                .datasetName(DATASET_NAME)
                .datasetChecksum(DATASET_CHECKSUM)
                .build()
                .containsDataset());

        assertFalse(TaskDescription.builder()
                .datasetAddress(DATASET_ADDRESS)
                .datasetUri(DATASET_URI)
                .datasetName(DATASET_NAME)
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
    void shouldContainInputFiles() {
        assertTrue(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .inputFiles(List.of("http://file1", "http://file2"))
                .build()
                .containsInputFiles());
    }

    @Test
    void shouldContainsInputFilesFromDealParams() {
        assertTrue(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .dealParams(DealParams.builder().iexecInputFiles(List.of("http://file1", "http://file2")).build())
                .build()
                .containsInputFiles());
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("provideTaskDescriptionWithoutInputFiles")
    void shouldNotContainInputFilesWhenNullDealParams(final DealParams dealParams) {
        assertFalse(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .dealParams(dealParams)
                .build()
                .containsInputFiles());
    }

    private static Stream<Arguments> provideTaskDescriptionWithoutInputFiles() {
        return Stream.of(
                Arguments.of(DealParams.builder().build()),
                Arguments.of(DealParams.builder().iexecInputFiles(null).build())
        );
    }

    @Test
    void shouldNotContainInputFilesWhenEmptyInputFiles() {
        assertFalse(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .inputFiles(List.of())
                .build()
                .containsInputFiles());
    }

    @Test
    void shouldNotContainInputFilesWhenNullInputFiles() {
        assertFalse(TaskDescription.builder()
                .chainTaskId(CHAIN_TASK_ID)
                .inputFiles(null)
                .build()
                .containsInputFiles());
    }
    // endregion

    // region getAppCommand
    @Test
    void shouldGenerateAppCommandWithEntrypointWhenEmptyDealParams() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .appEnclaveConfiguration(TeeEnclaveConfiguration.builder().entrypoint(ENTRYPOINT).build())
                .dealParams(DealParams.builder().build())
                .build();
        assertEquals(ENTRYPOINT, taskDescription.getAppCommand());
    }

    @Test
    void shouldGenerateAppCommandWithEntrypointWhenNullDealParams() {
        final TaskDescription taskDescription = TaskDescription.builder()
                .appEnclaveConfiguration(TeeEnclaveConfiguration.builder().entrypoint(ENTRYPOINT).build())
                .dealParams(null)
                .build();
        assertEquals(ENTRYPOINT, taskDescription.getAppCommand());
    }

    @Test
    void shouldGenerateAppCommandWithEntrypointAndArgs() {
        assertEquals(ENTRYPOINT + " " + CMD, TaskDescription.builder()
                .appEnclaveConfiguration(TeeEnclaveConfiguration.builder().entrypoint(ENTRYPOINT).build())
                .dealParams(DealParams.builder().iexecArgs(CMD).build())
                .build()
                .getAppCommand());
        assertEquals(ENTRYPOINT + " " + CMD, TaskDescription.builder()
                .appEnclaveConfiguration(TeeEnclaveConfiguration.builder().entrypoint(ENTRYPOINT).build())
                .dealParams(DealParams.builder().build())
                .cmd(CMD)
                .build()
                .getAppCommand());
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
