/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.eip712.EIP712Entity;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.EthAddress;
import com.iexec.commons.poco.utils.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.*;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Slf4j
public class SignerService {

    private final Credentials credentials;
    private final RawTransactionManager txManager;
    private final Web3j web3j;

    public SignerService(Web3j web3j, long chainId) throws Exception {
        try {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            credentials = Credentials.create(ecKeyPair);
            log.info("Created new wallet credentials [address:{}] ", credentials.getAddress());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            log.error("Cannot create new wallet credentials", e);
            throw e;
        }
        this.txManager = new RawTransactionManager(
                web3j, credentials, chainId, 10, 1000L);
        this.web3j = web3j;
    }

    public SignerService(Web3j web3j, long chainId, Credentials credentials) {
        this.credentials = credentials;
        if (credentials != null && EthAddress.validate(credentials.getAddress())) {
            log.info("Loaded wallet credentials [address:{}]",
                    credentials.getAddress());
        } else {
            throw new ExceptionInInitializerError("Cannot create credential service");
        }
        this.txManager = new RawTransactionManager(
                web3j, credentials, chainId, 10, 1000L);
        this.web3j = web3j;
    }

    public SignerService(Web3j web3j, long chainId, String walletPassword, String walletPath) throws Exception {
        try {
            credentials = WalletUtils.loadCredentials(walletPassword, walletPath);
            log.info("Loaded wallet credentials [address:{}] ", credentials.getAddress());
        } catch (IOException | CipherException e) {
            log.error("Cannot load wallet credentials", e);
            throw e;
        }
        this.txManager = new RawTransactionManager(
                web3j, credentials, chainId, 10, 1000L);
        this.web3j = web3j;
    }

    public String getAddress() {
        return credentials.getAddress();
    }

    /**
     * Signs messages with Ethereum prefix
     */
    public Signature signMessageHash(String messageHash) {
        String hexPrivateKey = Numeric.toHexStringWithPrefix(credentials.getEcKeyPair().getPrivateKey());
        return SignatureUtils.signMessageHashAndGetSignature(messageHash, hexPrivateKey);
    }

    public String signEIP712Entity(EIP712Entity<?> eip712Entity) {
        final String signature = eip712Entity.signMessage(credentials.getEcKeyPair());
        if (StringUtils.isEmpty(signature)) {
            log.error("Empty signature [entity:{}]", eip712Entity);
            return null;
        }
        return signature;
    }

    /**
     * Builds an authorization token for given {@link EIP712Entity}.
     * <p>
     * An authorization token is the concatenation of the following values, delimited by an underscore:
     * <ol>
     * <li>Entity hash
     * <li>Signed message
     * <li>Credentials address
     * </ol>
     *
     * @param eip712Entity Entity to sign a token for.
     * @return The authorization token.
     */
    public String signEIP712EntityAndBuildToken(EIP712Entity<?> eip712Entity) {
        final String hash = eip712Entity.getHash();
        final String signedMessage = signEIP712Entity(eip712Entity);
        return signedMessage == null ? null : String.join("_", hash, signedMessage, credentials.getAddress());
    }

    /**
     * Sign and send transaction for signer
     *
     * @param nonce
     * @param gasPrice
     * @param gasLimit
     * @param to
     * @param data
     * @return transaction hash
     * @throws IOException if communication with the blockchain network failed
     */
    public String signAndSendTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, String data) throws IOException {
        RawTransaction rawTx = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, to, BigInteger.ZERO, data);
        EthSendTransaction transactionResponse = txManager.signAndSend(rawTx);
        if (transactionResponse.hasError()) {
            final Response.Error responseError = transactionResponse.getError();
            log.error("transaction failed [message:{}, code:{}, data:{}]",
                    responseError.getMessage(), responseError.getCode(), responseError.getData());
            throw new JsonRpcError(responseError);
        }
        String expectedTxHash = transactionResponse.getTransactionHash();
        log.debug("verifying tx is in mem-pool {}", expectedTxHash);
        return web3j.ethGetTransactionByHash(expectedTxHash).send()
                .getTransaction()
                .map(Transaction::getHash)
                .orElse("");
    }
}
