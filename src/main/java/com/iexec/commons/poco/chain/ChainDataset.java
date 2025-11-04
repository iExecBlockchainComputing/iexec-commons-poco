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
import com.iexec.commons.poco.utils.MultiAddressHelper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import static com.iexec.commons.poco.chain.Web3jAbstractService.toBigInt;

@Slf4j
@Value
@Builder
public class ChainDataset {
    String chainDatasetId;
    String multiaddr;
    String checksum;

    public static ChainDataset fromRawData(final String address, final String rawData) {
        log.debug("ChainDataset.fromRawData [address:{}]", address);
        final String[] parts = PoCoDataDecoder.toParts(rawData);
        final int offset = toBigInt(parts[0]).intValue() / 32;
        final int multiaddrOffset = toBigInt(parts[offset + 2]).intValue() / 32;
        final String multiaddr = PoCoDataDecoder.decodeToHexString(parts, offset + multiaddrOffset);
        return ChainDataset.builder()
                .chainDatasetId(address)
                .multiaddr(MultiAddressHelper.convertToURI(multiaddr))
                .checksum("0x" + parts[offset + 3])
                .build();
    }
}
