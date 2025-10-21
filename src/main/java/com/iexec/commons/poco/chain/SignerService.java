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

import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.eip712.EIP712Entity;
import com.iexec.commons.poco.eip712.EIP712TypedData;
import com.iexec.commons.poco.order.Order;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.EthAddress;
import com.iexec.commons.poco.utils.SignatureUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.*;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.tx.RawTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

@Slf4j
public class SignerService {

    public static final String GENERIC_EVM_ERROR_MESSAGE = "VM execution error.";

    @Getter
    private final Credentials credentials;
    private final RawTransactionManager txManager;
    private final Web3j web3j;

    public SignerService(Web3j web3j, long chainId) throws GeneralSecurityException {
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
        if (credentials != null && EthAddress.validate(credentials.getAddress())) {
            log.info("Loaded wallet credentials [address:{}]",
                    credentials.getAddress());
        } else {
            throw new ExceptionInInitializerError("Cannot create credential service");
        }
        this.credentials = credentials;
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

    public BigInteger getNonce() {
        try {
            return web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.PENDING)
                    .send().getTransactionCount();
        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

    /**
     * Signs messages with Ethereum prefix
     */
    public Signature signMessageHash(String messageHash) {
        return SignatureUtils.signMessageHashAndGetSignature(messageHash, credentials.getEcKeyPair());
    }

    /**
     * @deprecated use signTypedDataForDomain instead
     */
    @Deprecated(forRemoval = true)
    public String signEIP712Entity(EIP712Entity<?> eip712Entity) {
        final String signature = eip712Entity.signMessage(credentials.getEcKeyPair());
        if (StringUtils.isEmpty(signature)) {
            log.error("Empty signature [entity:{}]", eip712Entity);
            return null;
        }
        return signature;
    }

    /**
     * Hashes and signs structured type data following EIP-712
     *
     * @param typedData structured data implementing {@link EIP712TypedData} to hash and sign
     * @param domain    EIP712 domain describing the target for which the data is hashed and signed
     * @return a valid signature
     * @see <a href="https://eips.ethereum.org/EIPS/eip-712">EIP-712</a>
     */
    public String signTypedDataForDomain(final EIP712TypedData typedData, final EIP712Domain domain) {
        return typedData.sign(credentials.getEcKeyPair(), domain);
    }

    public Order signOrderForDomain(final Order order, final EIP712Domain domain) {
        final String sig = signTypedDataForDomain(order, domain);
        return order.withSignature(sig);
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

    // TODO keep single instance with the one in Web3jAbstractService
    private void handleError(final Response.Error error, final String label) {
        log.error("{}} failed [message:{}, code:{}, data:{}]",
                label, error.getMessage(), error.getCode(), error.getData());
        final String revertMessage = GENERIC_EVM_ERROR_MESSAGE.equals(error.getMessage()) ? error.getData() : error.getMessage();
        final Pattern p = Pattern.compile("^\"?Reverted (0x[0-9A-Fa-f]+)\"?$");
        final Matcher m = p.matcher(revertMessage);
        log.debug("matcher matches {} {}", m.matches(), m.groupCount());
        final String message = m.matches() ? BytesUtils.hexStringToAscii(m.group(1)) : revertMessage;
        throw new JsonRpcError(error.getCode(), message, null);
    }

    /**
     * Sends an {@code eth_call} to an Ethereum address.
     * <p>
     * The call is synchronous and does not modify the blockchain state.
     * <p>
     * The {@code sendCall} method can throw runtime exceptions, specifically {@code ContractCallException}.
     * Those exceptions must be caught and handled properly in the business code.
     *
     * @param to   Contract address to send the call to
     * @param data Encoded data representing the method to call with its parameters
     * @return A single value returned by the called method.
     * @throws IOException in case of communication failure with the blockchain network.
     * @see <a href="https://ethereum.org/en/developers/docs/apis/json-rpc/#eth_call">eth_call JSON-RPC API</a>
     */
    public String sendCall(String to, String data) throws IOException {
        return sendCall(to, data, DefaultBlockParameterName.LATEST);
    }

    public String sendCall(String to, String data, DefaultBlockParameter defaultBlockParameter) throws IOException {
        final EthCall ethCall = web3j.ethCall(
                createEthCallTransaction(credentials.getAddress(), to, data), defaultBlockParameter).send();
        if (ethCall.hasError()) {
            handleError(ethCall.getError(), "ethCall");
        }
        log.debug("ethCall [value:{}]", ethCall.getValue());
        return ethCall.getValue();
    }

    /**
     * Estimates Gas amount to be used to mine the transaction described by the given payload.
     *
     * @param to   Contract address to send the call to
     * @param data Encoded data representing the method to estimate with its parameters
     * @return The estimated Gas amount needed to mine the transaction
     * @throws IOException in case of communication failure with the blockchain network
     * @see <a href="https://ethereum.org/en/developers/docs/apis/json-rpc/#eth_estimategas">eth_estimateGas JSON RPC-API</a>
     */
    public BigInteger estimateGas(String to, String data) throws IOException {
        final EthEstimateGas estimateGas = web3j.ethEstimateGas(
                createEthCallTransaction(credentials.getAddress(), to, data)).send();
        if (estimateGas.hasError()) {
            handleError(estimateGas.getError(), "estimateGas");
        }
        log.debug("estimateGas [amountUsed:{}]", estimateGas.getAmountUsed());
        return estimateGas.getAmountUsed();
    }

    /**
     * Sign and send a new transaction for signer on the blockchain network
     *
     * @param nonce    transaction counter of the account
     * @param gasPrice price paid per gas unit consumed for the transaction
     * @param gasLimit threshold limiting the gas quantity that can be spent on the transaction
     * @param to       target ethereum address
     * @param data     function selector with encoded arguments
     * @return the submitted transaction hash
     * @throws IOException if communication with the blockchain network failed
     */
    public String signAndSendTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, String data) throws IOException {
        final RawTransaction rawTx = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, to, BigInteger.ZERO, data);
        final EthSendTransaction transactionResponse = txManager.signAndSend(rawTx);
        if (transactionResponse.hasError()) {
            final Response.Error responseError = transactionResponse.getError();
            log.error("Transaction failed [message:{}, code:{}, data:{}]",
                    responseError.getMessage(), responseError.getCode(), responseError.getData());
            throw new JsonRpcError(responseError);
        }
        final String txHash = transactionResponse.getTransactionHash();
        log.info("Transaction submitted [nonce:{}, txHash:{}]", nonce, txHash);
        return txHash;
    }

    public String signAndSendTransaction(BigInteger nonce, BigInteger gasPrice, String to, String data) throws IOException {
        final BigInteger gasLimit = estimateGas(to, data);
        return signAndSendTransaction(nonce, gasPrice, gasLimit, to, data);
    }

    /**
     * Verifies a transaction is found on-chain by its hash
     *
     * @param txHash hash of the transaction
     * @return {@literal true} if the transaction was found, {@literal false} otherwise
     */
    public boolean verifyTransaction(final String txHash) {
        if (!BytesUtils.isNonZeroedBytes32(txHash)) {
            log.warn("Invalid transaction hash [txHash:{}]", txHash);
            return false;
        }
        try {
            log.debug("Verifying transaction is in mem-pool [txHash:{}]", txHash);
            return web3j.ethGetTransactionByHash(txHash).send()
                    .getTransaction()
                    .map(Transaction::getHash)
                    .orElse("")
                    .equalsIgnoreCase(txHash);
        } catch (Exception e) {
            log.warn("Transaction verification failed [txHash:{}]", txHash, e);
        }
        return false;
    }
}
