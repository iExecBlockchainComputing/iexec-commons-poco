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

package com.iexec.commons.poco.chain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.HashUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerpoolAuthorization {

    private String chainTaskId;
    private String workerWallet;
    private String enclaveChallenge;
    private Signature signature;

    @JsonIgnore
    public String getHash() {
        return HashUtils.concatenateAndHash(workerWallet, chainTaskId, enclaveChallenge);
    }
}