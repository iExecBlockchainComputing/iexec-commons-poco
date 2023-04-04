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

import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.HashUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeeEnclaveChallengeSignature {

    private String resultDigest;
    private String resultHash;
    private String resultSeal;
    private Signature signature;

    public static String getMessageHash(String resultHash, String resultSeal) {
        return HashUtils.concatenateAndHash(resultHash, resultSeal);
    }
}