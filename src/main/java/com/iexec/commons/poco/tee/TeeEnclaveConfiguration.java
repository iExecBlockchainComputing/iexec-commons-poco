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

package com.iexec.commons.poco.tee;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class TeeEnclaveConfiguration {

    @JsonAlias("provider")
    TeeFramework framework;
    String version;
    String entrypoint;
    long heapSize;
    String fingerprint;

    public static TeeEnclaveConfiguration buildEnclaveConfigurationFromJsonString(String jsonString)
            throws JsonProcessingException {
        return new ObjectMapper()
                .readValue(jsonString, TeeEnclaveConfiguration.class);
    }

    public String toJsonString() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    @JsonIgnore
    public TeeEnclaveConfigurationValidator getValidator() {
        return new TeeEnclaveConfigurationValidator(this);
    }
}
