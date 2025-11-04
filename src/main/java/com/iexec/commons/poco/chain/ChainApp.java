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

import com.iexec.commons.poco.encoding.PoCoDataDecoder;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import static com.iexec.commons.poco.chain.Web3jAbstractService.toBigInt;

@Slf4j
@Value
@Builder
public class ChainApp {
    String chainAppId;
    String type;
    String multiaddr;
    String checksum;
    TeeEnclaveConfiguration enclaveConfiguration;

    public static ChainApp fromRawData(final String address, final String rawData) {
        log.debug("ChainApp.fromRawData [address:{}]", address);
        final String[] parts = PoCoDataDecoder.toParts(rawData);
        final int offset = toBigInt(parts[0]).intValue() / 32;
        final int typeOffset = toBigInt(parts[offset + 2]).intValue() / 32;
        final int multiaddrOffest = toBigInt(parts[offset + 3]).intValue() / 32;
        final int enclaveOffset = toBigInt(parts[offset + 5]).intValue() / 32;
        final String enclaveContrib = PoCoDataDecoder.decodeToAsciiString(parts, offset + enclaveOffset);
        return ChainApp.builder()
                .chainAppId(address)
                .type(PoCoDataDecoder.decodeToAsciiString(parts, offset + typeOffset))
                .multiaddr(PoCoDataDecoder.decodeToAsciiString(parts, offset + multiaddrOffest))
                .checksum("0x" + parts[offset + 4])
                .enclaveConfiguration(TeeEnclaveConfiguration.fromJsonString(enclaveContrib))
                .build();
    }
}
