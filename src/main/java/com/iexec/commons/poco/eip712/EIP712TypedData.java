/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.utils.HashUtils;
import com.iexec.commons.poco.utils.SignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;

public interface EIP712TypedData {
    Logger log = LoggerFactory.getLogger(EIP712TypedData.class);

    default String computeHash(final EIP712Domain domain) {
        final String domainSeparator = domain.getDomainSeparator();
        final String messageHash = computeMessageHash();
        final String hash = HashUtils.concatenateAndHash("0x1901", domainSeparator, messageHash);
        if (log.isDebugEnabled()) {
            log.debug("domainSeparator {}", domainSeparator);
            log.debug("messageHash {}", messageHash);
            log.debug("hash {}", hash);
        }
        return hash;
    }

    String computeMessageHash();

    default String sign(final ECKeyPair ecKeyPair, final EIP712Domain domain) {
        return SignatureUtils.signAsString(computeHash(domain), ecKeyPair);
    }
}
