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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DealParamsTest {

    private static final String ARGS = "argument for the worker";
    private static final String FILE1 = "http://test.com/image1.png";
    private static final String FILE2 = "http://test.com/image2.png";
    private static final String FILE3 = "http://test.com/image3.png";

    @Test
    void shouldReadArgsWithoutJson() {
        final DealParams params = DealParams.createFromString(ARGS);
        assertEquals(ARGS, params.getIexecArgs());
        assertThat(params.getIexecInputFiles()).isNotNull();
        assertThat(params.getIexecInputFiles()).isEmpty();
        assertThat(params.getIexecSecrets()).isNotNull();
        assertThat(params.getIexecSecrets()).isEmpty();
    }

    @Test
    void shouldReadArgsInJson() {
        final DealParams params = DealParams.createFromString("{\"iexec_args\":\"" + ARGS + "\"}");
        assertThat(params.getIexecArgs()).isEqualTo(ARGS);
        assertThat(params.getIexecInputFiles()).isNotNull();
        assertThat(params.getIexecInputFiles()).isEmpty();
        assertThat(params.getIexecSecrets()).isNotNull();
        assertThat(params.getIexecSecrets()).isEmpty();
    }

    @Test
    void shouldReadArgsInJsonAndEmptyInputFilesAndEmptySecrets() {
        final DealParams params = DealParams.createFromString("{\"iexec_args\":\"" + ARGS + "\"," +
                "\"iexec_input_files\":[],\"iexec_secrets\":{}}");
        assertThat(params.getIexecArgs()).isEqualTo(ARGS);
        assertThat(params.getIexecInputFiles()).isNotNull();
        assertThat(params.getIexecInputFiles()).isEmpty();
        assertThat(params.getIexecSecrets()).isNotNull();
        assertThat(params.getIexecSecrets()).isEmpty();
    }

    @Test
    void shouldReadNotCorrectJsonFile() {
        final String wrongJson = "{\"wrong_field1\":\"wrong arg value\"," +
                "\"iexec_input_files\":[\"http://file1\"]}";
        final DealParams params = DealParams.createFromString(wrongJson);
        assertThat(params.getIexecArgs()).isEqualTo(wrongJson);
        assertThat(params.getIexecInputFiles()).isNotNull();
        assertThat(params.getIexecInputFiles()).isEmpty();
    }

    @Test
    void testSerializationWithBuilderDefaultValues() {
        final DealParams params = DealParams.builder().build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(newParams)
                .usingRecursiveComparison()
                .isEqualTo(params);
        assertThat(newParams.getIexecInputFiles()).isEqualTo(Collections.emptyList());
        assertThat(newParams.getIexecSecrets()).isEqualTo(Collections.emptyMap());
    }

    @Test
    void testSerializationWithBooleanAsFalse() {
        final DealParams params = DealParams.builder()
                .iexecResultEncryption(false)
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(newParams)
                .usingRecursiveComparison()
                .isEqualTo(params);
        assertThat(newParams.getIexecInputFiles()).isEqualTo(Collections.emptyList());
        assertThat(newParams.getIexecSecrets()).isEqualTo(Collections.emptyMap());
    }

    @Test
    void testSerializationWithBooleanAsTrue() {
        final DealParams params = DealParams.builder()
                .iexecResultEncryption(true)
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(newParams)
                .usingRecursiveComparison()
                .isEqualTo(params);
        assertThat(newParams.getIexecInputFiles()).isEqualTo(Collections.emptyList());
        assertThat(newParams.getIexecSecrets()).isEqualTo(Collections.emptyMap());
    }

    @Test
    void testSerializationForInputFiles() {
        final DealParams params = DealParams.builder()
                .iexecInputFiles(List.of(FILE1, FILE2))
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(params)
                .usingRecursiveComparison()
                .isEqualTo(newParams);
        assertThat(newParams.getIexecSecrets())
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testSerializationForSecrets() {
        final DealParams params = DealParams.builder()
                .iexecSecrets(Map.of("0", "secretA", "3", "secretX"))
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(params)
                .usingRecursiveComparison()
                .isEqualTo(newParams);
        assertThat(newParams.getIexecInputFiles())
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testSerializationWithEmptyArgs() {
        final DealParams params = DealParams.builder()
                .iexecArgs("")
                .iexecInputFiles(Collections.emptyList())
                .iexecSecrets(Collections.emptyMap())
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(params)
                .usingRecursiveComparison()
                .ignoringFields("iexecArgs")
                .isEqualTo(newParams);
        assertThat(params.getIexecArgs()).isNotNull();
        assertThat(params.getIexecArgs()).isBlank();
        assertThat(newParams.getIexecArgs()).isNull();
        assertThat(params.getIexecInputFiles()).isEmpty();
        assertThat(newParams.getIexecInputFiles()).isEmpty();
        assertThat(params.getIexecSecrets()).isEmpty();
        assertThat(newParams.getIexecSecrets()).isEmpty();
    }

    @Test
    void testSerializationWithArgsAndMultipleFilesAndMultipleSecrets() {
        final DealParams params = DealParams.builder()
                .iexecArgs(ARGS)
                .iexecInputFiles(List.of(FILE2, FILE3))
                .iexecSecrets(Map.of("2", "secretK", "1", "secretD"))
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(params).isEqualTo(newParams);
    }

    @ParameterizedTest
    @ValueSource(strings = {DealParams.DROPBOX_RESULT_STORAGE_PROVIDER, DealParams.IPFS_RESULT_STORAGE_PROVIDER})
    void testSerializationWithAllParams(String storageProvider) {
        final DealParams params = DealParams.builder()
                .iexecArgs(ARGS)
                .iexecInputFiles(List.of(FILE3, FILE2, FILE1))
                .iexecSecrets(Map.of("1", "secretC", "2", "secretB", "3", "secretA"))
                .iexecResultEncryption(true)
                .iexecResultStorageProvider(storageProvider)
                .iexecResultStorageProxy("http://result-proxy.local:13200")
                .build();
        final DealParams newParams = DealParams.createFromString(params.toJsonString());
        assertThat(newParams).isEqualTo(params);
        assertThat(newParams.getIexecArgs()).isNotBlank();
        assertThat(newParams.getIexecInputFiles()).isNotEmpty();
        assertThat(newParams.getIexecSecrets()).isNotEmpty();
    }

}
