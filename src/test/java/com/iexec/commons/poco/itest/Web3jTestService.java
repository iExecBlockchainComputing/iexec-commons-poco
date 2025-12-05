/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.chain.Web3jAbstractService;
import com.iexec.commons.poco.encoding.AssetDataEncoder;
import com.iexec.commons.poco.encoding.LogTopic;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class Web3jTestService extends Web3jAbstractService {
    static final long BLOCK_TIME = 5;
    static final long MINING_TIMEOUT = 2 * BLOCK_TIME;

    public Web3jTestService(String chainNodeAddress) {
        this(chainNodeAddress, 1.0f, 0L);
    }

    public Web3jTestService(String chainNodeAddress, float gasPriceMultiplier, long gasPriceCap) {
        this(chainNodeAddress, gasPriceMultiplier, gasPriceCap, true);
    }

    public Web3jTestService(String chainNodeAddress, float gasPriceMultiplier, long gasPriceCap, boolean isSidechain) {
        super(65535, chainNodeAddress, Duration.ofSeconds(BLOCK_TIME), gasPriceMultiplier, gasPriceCap, isSidechain);
    }

    @SneakyThrows
    SignerService createSigner() {
        final ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        final Credentials credentials = Credentials.create(ecKeyPair);
        return new SignerService(this.getWeb3j(), this.getChainId(), credentials);
    }

    public boolean areTxMined(final String... txHashes) {
        return txHashes.length == 0 || Stream.of(txHashes)
                .map(this::getTransactionReceipt)
                .map(Objects::nonNull)
                .reduce(Boolean::logicalAnd)
                .orElse(false);
    }

    public boolean areTxStatusOK(final String... txHashes) {
        return txHashes.length == 0 || Stream.of(txHashes)
                .map(this::getTransactionReceipt)
                .map(this::isTxStatusOK)
                .reduce(Boolean::logicalAnd)
                .orElse(false);
    }

    private boolean isTxStatusOK(final TransactionReceipt receipt) {
        return receipt != null && receipt.isStatusOK();
    }

    public List<String> getDeployedAssets(final String... txHashes) {
        return Stream.of(txHashes)
                .map(this::getTransactionReceipt)
                .map(AssetDataEncoder::getAssetAddressFromReceipt)
                .toList();
    }

    public void showReceipt(final String txHash, final String method) {
        final TransactionReceipt receipt = this.getTransactionReceipt(txHash);
        log.info("{} receipt gas {}", method, receipt.getGasUsed());
        log.info("{} receipt log events {}",
                method, receipt.getLogs().stream().map(log -> LogTopic.decode(log.getTopics().get(0))).toList());
    }
}
