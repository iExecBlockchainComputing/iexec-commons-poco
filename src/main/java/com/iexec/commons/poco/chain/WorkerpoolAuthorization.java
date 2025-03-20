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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import lombok.*;

@Value
@AllArgsConstructor
@Builder
@JsonDeserialize(builder = WorkerpoolAuthorization.WorkerpoolAuthorizationBuilder.class)
public class WorkerpoolAuthorization {

    @Builder.Default
    String chainTaskId = BytesUtils.EMPTY_HEX_STRING_32;
    @Builder.Default
    String dealId = BytesUtils.EMPTY_HEX_STRING_32;
    @Builder.Default
    int taskIndex = 0;
    String workerWallet;
    String enclaveChallenge;
    Signature signature;

    @JsonIgnore
    public String getHash() {
        return HashUtils.concatenateAndHash(workerWallet, chainTaskId, enclaveChallenge);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class WorkerpoolAuthorizationBuilder {}
}
