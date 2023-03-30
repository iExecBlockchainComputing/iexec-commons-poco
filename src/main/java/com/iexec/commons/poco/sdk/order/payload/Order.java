/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.sdk.order.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Order {

    BigInteger volume;
    String tag;
    String salt;
    String sign;

    @JsonIgnore
    public boolean isSigned() {
        return sign != null && !sign.isEmpty();
    }

    /**
     * Converts all characters to Lowercase or returns empty
     * @param s String to convert
     * @return the String, converted to lowercase or an empty string
     */
    public String toLowerCase(String s) {
        return s != null ? s.toLowerCase() : "";
    }

}

