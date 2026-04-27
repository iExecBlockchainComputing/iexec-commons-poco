/*
 * Copyright 2022-2026 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.eip712.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.eip712.TypeParam;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class EIP712Challenge {

    private Map<String, List<TypeParam>> types;
    private String primaryType;
    private EIP712Domain domain;
    private Challenge message;

    public EIP712Challenge(final EIP712Domain domain, final Challenge message) {
        this.types = new LinkedHashMap<>();
        this.types.put(EIP712Domain.primaryType, domain.getTypes());
        this.types.put("Challenge", getMessageTypeParams());
        this.primaryType = "Challenge";
        this.domain = domain;
        this.message = message;
    }

    @JsonIgnore
    public List<TypeParam> getMessageTypeParams() {
        return List.of(new TypeParam("challenge", "string"));
    }

    @JsonIgnore
    public String getMessageHash() {
        return message.computeMessageHash();
    }

    @JsonIgnore
    public List<TypeParam> getDomainTypeParams() {
        return types.get(EIP712Domain.primaryType);
    }

    @JsonIgnore
    public String getHash() {
        return message.computeHash(domain);
    }

}
