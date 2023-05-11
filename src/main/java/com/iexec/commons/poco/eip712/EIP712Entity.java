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
import com.iexec.commons.poco.utils.HashUtils;
import com.iexec.commons.poco.utils.SignatureUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.ECKeyPair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@NoArgsConstructor
public abstract class EIP712Entity<M> implements EIP712<M> {

    private Map<String, List<TypeParam>> types;
    private EIP712Domain domain;
    private M message;

    protected EIP712Entity(EIP712Domain domain, M message) {
        this.domain = domain;
        this.message = message;
        this.types = Map.of(
                EIP712Domain.primaryType, domain.getTypes(),
                getPrimaryType(), getMessageTypeParams()
        );
    }

    @Override
    public Map<String, List<TypeParam>> getTypes() {
        return new HashMap<>(types);
    }

    @Override
    public EIP712Domain getDomain() {
        return domain;
    }

    @Override
    public M getMessage() {
        return message;
    }

    @Override
    public String getHash() {
        String domainSeparator = getDomain().getDomainSeparator();
        String messageHash = getMessageHash();
        log.info("domainSeparator {}", domainSeparator);
        log.info("messageHash {}", messageHash);
        String hash = HashUtils.concatenateAndHash(
                "0x1901",
                domainSeparator,
                messageHash);
        log.info("hash {}", hash);
        return hash;
    }

    public String hashMessageValues(Object... values) {
        String type = getPrimaryType() + "(" + getMessageTypeParams().stream()
                .map(TypeParam::toDescription)
                .collect(Collectors.joining(",")) + ")";
        //MyEntity(address param1, string param2, ..)
        String[] encodedValues = Stream.concat(Stream.of(type), Arrays.stream(values))
                .map(EIP712Utils::encodeData)
                .toArray(String[]::new);
        if (log.isDebugEnabled()) {
            log.debug("{}", type);
            for (String value : encodedValues) {
                log.debug("{}", value);
            }
        }
        return HashUtils.concatenateAndHash(encodedValues);
    }

    public String signMessage(ECKeyPair ecKeyPair) {
        return SignatureUtils.signAsString(this.getHash(), ecKeyPair);
    }

    @JsonIgnore
    public List<TypeParam> getDomainTypeParams() {
        return new ArrayList<>(types.get(EIP712Domain.primaryType));
    }
}
