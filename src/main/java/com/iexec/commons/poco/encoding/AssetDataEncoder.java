/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.encoding;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.DynamicBytes;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static com.iexec.commons.poco.encoding.Utils.toHexString;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssetDataEncoder {

    private static final String CREATE_APP_SELECTOR = "0x3f7868ff";
    private static final String CREATE_DATASET_SELECTOR = "0x3354bcdb";
    private static final String CREATE_WORKERPOOL_SELECTOR = "0xe40238f4";

    /**
     * Encodes data for {@code createApp} transaction.
     *
     * @param owner     Owner Ethereum address
     * @param name      App name
     * @param type      App type, unique supported value should be DOCKER
     * @param multiaddr Docker registry download address
     * @param checksum  Docker image SAH256 checksum
     * @param mrenclave TEE configuration if applicable, should be a valid JSON string of {@link com.iexec.commons.poco.tee.TeeEnclaveConfiguration}
     * @return encoded data
     */
    public static String encodeApp(String owner, String name, String type, String multiaddr, String checksum, String mrenclave) {
        long offset = 6;
        StringBuilder sb = new StringBuilder(CREATE_APP_SELECTOR);
        sb.append(toHexString(owner));

        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        String nameContrib = TypeEncoder.encode(new DynamicBytes(name.getBytes(StandardCharsets.UTF_8)));

        offset += nameContrib.length() / 64;
        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        String typeContrib = TypeEncoder.encode(new DynamicBytes(type.getBytes(StandardCharsets.UTF_8)));

        offset += typeContrib.length() / 64;
        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        String multiaddrContrib = TypeEncoder.encode(new DynamicBytes(multiaddr.getBytes(StandardCharsets.UTF_8)));

        sb.append(checksum);

        offset += multiaddrContrib.length() / 64;
        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        String mrenclaveContrib = TypeEncoder.encode(new DynamicBytes(mrenclave.getBytes(StandardCharsets.UTF_8)));

        sb.append(nameContrib);
        sb.append(typeContrib);
        sb.append(multiaddrContrib);
        sb.append(mrenclaveContrib);
        log.debug("app tx {}", sb);
        return sb.toString();
    }

    /**
     * Encodes data for {@code createDataset} transaction.
     *
     * @param owner     Owner Ethereum address
     * @param name      Dataset name, can be empty
     * @param multiaddr Download address, can be a http link or an IPFS multiaddr
     * @param checksum  SHA256 checksum of the dataset
     * @return encoded data
     */
    public static String encodeDataset(String owner, String name, String multiaddr, String checksum) {
        long offset = 4;
        StringBuilder sb = new StringBuilder(CREATE_DATASET_SELECTOR);

        String datasetNameOffset = toHexString(BigInteger.valueOf(offset * 32));
        String datasetNameContrib = TypeEncoder.encode(new DynamicBytes(name.getBytes(StandardCharsets.UTF_8)));
        offset += datasetNameContrib.length() / 64;

        String datasetAddrOffset = toHexString(BigInteger.valueOf(offset * 32));
        String datasetAddrContrib = TypeEncoder.encode(new DynamicBytes(multiaddr.getBytes(StandardCharsets.UTF_8)));

        sb.append(toHexString(owner));
        sb.append(datasetNameOffset);
        sb.append(datasetAddrOffset);
        sb.append(checksum);
        sb.append(datasetNameContrib);
        sb.append(datasetAddrContrib);
        log.debug("dataset tx {}", sb);
        return sb.toString();
    }

    /**
     * Encodes data for {@code createWorkerpool} transaction.
     *
     * @param owner       Owner Ethereum address
     * @param description Workerpool description
     * @return encoded data
     */
    public static String encodeWorkerpool(String owner, String description) {
        long offset = 2;
        StringBuilder sb = new StringBuilder(CREATE_WORKERPOOL_SELECTOR);
        sb.append(toHexString(owner));
        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        String descriptionContrib = TypeEncoder.encode(new DynamicBytes(description.getBytes(StandardCharsets.UTF_8)));
        sb.append(descriptionContrib);
        log.debug("workerpool tx {}", sb);
        return sb.toString();
    }
}
