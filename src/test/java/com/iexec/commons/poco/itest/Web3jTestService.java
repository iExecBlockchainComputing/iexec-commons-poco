/*
 * Copyright 2023-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.itest;

import com.iexec.commons.poco.chain.Web3jAbstractService;
import com.iexec.commons.poco.encoding.AssetDataEncoder;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Web3jTestService extends Web3jAbstractService {
    static long BLOCK_TIME = 5;

    public Web3jTestService(String chainNodeAddress) {
        super(65535, chainNodeAddress, Duration.ofSeconds(BLOCK_TIME), 1.0f, 22_000_000_000L, true);
    }

    public boolean areTxMined(String... txHashes) {
        return Stream.of(txHashes)
                .map(this::getTransactionReceipt)
                .map(Objects::nonNull)
                .reduce(Boolean::logicalAnd)
                .orElse(false);
    }

    public boolean areTxStatusOK(String... txHash) {
        return Stream.of(txHash)
                .map(this::getTransactionReceipt)
                .filter(Objects::nonNull)
                .map(TransactionReceipt::isStatusOK)
                .reduce(Boolean::logicalAnd)
                .orElse(false);
    }

    void displayGas(String method, BigInteger estimated, String txHash) {
        final TransactionReceipt receipt = getTransactionReceipt(txHash);
        log.info("Gas used [method:{}, estimated:{}, consumed:{}]", method, estimated, receipt.getGasUsed());
    }

    public List<String> getDeployedAssets(String... txHashes) {
        return Stream.of(txHashes)
                .map(this::getTransactionReceipt)
                .map(AssetDataEncoder::getAssetAddressFromReceipt)
                .collect(Collectors.toList());
    }
}
