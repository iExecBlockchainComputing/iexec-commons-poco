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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerpoolAuthorizationTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeWorkerpoolAuthorization() throws JsonProcessingException {
        final Signature signature = new Signature("0xsignature");
        final WorkerpoolAuthorization wpAuthorization = WorkerpoolAuthorization.builder()
                .chainTaskId("chainTaskId")
                .dealId("dealId")
                .taskIndex(1)
                .workerWallet("workerWallet")
                .enclaveChallenge("enclaveChallenge")
                .signature(signature)
                .build();
        final String jsonString = mapper.writeValueAsString(wpAuthorization);
        final WorkerpoolAuthorization deserializedWpAuthorization = mapper.readValue(jsonString, WorkerpoolAuthorization.class);
        assertThat(deserializedWpAuthorization).usingRecursiveComparison().isEqualTo(wpAuthorization);
        assertThat(deserializedWpAuthorization.getChainTaskId()).isEqualTo("chainTaskId");
        assertThat(deserializedWpAuthorization.getDealId()).isEqualTo("dealId");
        assertThat(deserializedWpAuthorization.getTaskIndex()).isEqualTo(1);
        assertThat(deserializedWpAuthorization.getWorkerWallet()).isEqualTo("workerWallet");
        assertThat(deserializedWpAuthorization.getEnclaveChallenge()).isEqualTo("enclaveChallenge");
        assertThat(deserializedWpAuthorization.getSignature()).isEqualTo(signature);
    }

    @Test
    void shouldGetCorrectHash() {
        final WorkerpoolAuthorization wpAuthorization = WorkerpoolAuthorization.builder()
                .chainTaskId("chainTaskId")
                .workerWallet("workerWallet")
                .enclaveChallenge("enclaveChallenge")
                .build();
        final String expectedHash = HashUtils.concatenateAndHash(
                "workerWallet", "chainTaskId", "enclaveChallenge");
        assertThat(wpAuthorization.getHash()).isEqualTo(expectedHash);
    }

    @Test
    void shouldUseDefaultValues() {
        final WorkerpoolAuthorization wpAuthorization = WorkerpoolAuthorization.builder()
                .workerWallet("workerWallet")
                .build();
        assertThat(wpAuthorization.getChainTaskId()).isEqualTo(BytesUtils.EMPTY_HEX_STRING_32);
        assertThat(wpAuthorization.getDealId()).isEqualTo(BytesUtils.EMPTY_HEX_STRING_32);
        assertThat(wpAuthorization.getTaskIndex()).isZero();
        assertThat(wpAuthorization.getEnclaveChallenge()).isNull();
        assertThat(wpAuthorization.getSignature()).isNull();
    }

}
