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

package com.iexec.commons.poco.eip712;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.web3j.crypto.ECKeyPair;

import java.util.List;
import java.util.Map;

/**
 * @deprecated only used in {@link EIP712Entity}
 */
@Deprecated(forRemoval = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface EIP712<M> {

    @JsonProperty("types")
    Map<String, List<TypeParam>> getTypes();

    @JsonProperty("domain")
    EIP712Domain getDomain();

    @JsonProperty("primaryType")
    String getPrimaryType();

    @JsonProperty("message")
    M getMessage();

    @JsonIgnore
    String getMessageHash();

    @JsonIgnore
    List<TypeParam> getMessageTypeParams();

    @JsonIgnore
    String getHash();

    String signMessage(ECKeyPair ecKeyPair);

}
