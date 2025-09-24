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

package com.iexec.commons.poco.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iexec.commons.poco.eip712.EIP712TypedData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

@Slf4j
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Order implements EIP712TypedData {

    protected final BigInteger volume;
    protected final String tag;
    protected final String salt;
    protected final String sign;

    @JsonIgnore
    public boolean isSigned() {
        return sign != null && !sign.isEmpty();
    }

    /**
     * Converts all characters to Lowercase or returns empty
     *
     * @param s String to convert
     * @return the String, converted to lowercase or an empty string
     */
    public String toLowerCase(String s) {
        return s != null ? s.toLowerCase() : "";
    }

    public abstract Order withSignature(String signature);

}
