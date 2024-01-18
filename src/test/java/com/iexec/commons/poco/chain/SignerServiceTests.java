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

import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.eip712.entity.Challenge;
import com.iexec.commons.poco.eip712.entity.EIP712Challenge;
import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignerServiceTests {
    @TempDir
    File tempDir;

    private static final long CHAIN_ID = 65535L;
    private static final String WALLET_PASS = "wallet-pass";
    private static final Web3j WEB3J_CLIENT = Web3j.build(new HttpService("http://localhost:8545"));

    // region constructor
    @Test
    void shouldConstructFromCredentials() {
        final Credentials credentials = Credentials.create(Hash.sha3(""));
        final SignerService signer = new SignerService(WEB3J_CLIENT, CHAIN_ID, credentials);
        assertThat(signer.getAddress()).isEqualTo("0x9cce34f7ab185c7aba1b7c8140d620b4bda941d6");
    }

    @Test
    void shouldConstructWithRandomWallet() throws Exception {
        final SignerService signer = new SignerService(WEB3J_CLIENT, CHAIN_ID);
        assertThat(signer.getAddress()).isNotEmpty();
    }

    @Test
    void shouldNotConstructFromNullCredentials() {
        assertThrows(ExceptionInInitializerError.class, () -> new SignerService(WEB3J_CLIENT, CHAIN_ID, null));
    }

    @Test
    void shouldLoadCorrectCredentials() throws Exception {
        final String walletPath = createTempWallet();
        final SignerService signer = new SignerService(WEB3J_CLIENT, CHAIN_ID, WALLET_PASS, walletPath);
        Credentials credentials = WalletUtils.loadCredentials(WALLET_PASS, walletPath);
        assertThat(signer.getAddress()).isEqualTo(credentials.getAddress());
    }

    @Test
    void shouldThrowIOExceptionSinceWalletFileNotFind() {
        assertThrows(FileNotFoundException.class, () -> new SignerService(WEB3J_CLIENT, CHAIN_ID, WALLET_PASS, "dummy-path"));
    }

    @Test
    void shouldThrowIOExceptionSinceCorruptedWalletFile() throws Exception {
        final String walletPath = createTempWallet();
        final FileWriter fw = new FileWriter(walletPath);
        fw.write("{new: devilish corrupted content}");
        fw.close();
        assertThrows(IOException.class, () -> new SignerService(WEB3J_CLIENT, CHAIN_ID, WALLET_PASS, walletPath));
    }

    @Test
    void shouldThrowCipherExceptionSinceWrongPassword() throws Exception {
        final String wrongPass = "wrong-pass";
        final String walletPath = createTempWallet();
        assertThrows(CipherException.class, () -> new SignerService(WEB3J_CLIENT, CHAIN_ID, wrongPass, walletPath));
    }
    // endregion

    // region sign
    @Test
    void shouldSignMessageHash() {
        final Credentials credentials = Credentials.create("0x2a46e8c1535792f6689b10d5c882c9363910c30751ec193ae71ec71630077909");
        final SignerService signer = new SignerService(WEB3J_CLIENT, CHAIN_ID, credentials);
        final String messageHash = HashUtils.concatenateAndHash(TeeUtils.TEE_SCONE_ONLY_TAG);
        final String signedMessageHash = signer.signMessageHash(messageHash).getValue();
        assertThat(signedMessageHash)
                .isEqualTo("0x1c5adf7baca8d9809e74567bfaa3365e90f90fadd7f00c0f60f1cdc308c7f1ab5f8fd58eca8ad1b1ecb81580aea8fd094cb3aa50b6b5ee1e2afe29f296e42ea81c");
    }

    @Test
    void shouldBuildAuthorizationTokenFromCredentials() {
        final long chainId = 13L;
        final Challenge challenge = Challenge.builder().challenge("abcd").build();
        final EIP712Domain domain = new EIP712Domain("OTHER DOMAIN", "2", chainId, null);
        final EIP712Challenge eip712Challenge = new EIP712Challenge(domain, challenge);
        final Credentials credentials = Credentials.create("0x2a46e8c1535792f6689b10d5c882c9363910c30751ec193ae71ec71630077909");

        final String expectedToken = "0xe001855eda78679dfa4972de06d1cf28c630561e17fc6b075130ce688f448bfe" +
                "_0xc0b3f255c47783e482e1932923dc388cfb5a737ebebdcec04b8ad7ac427c8c9d5155c3211a375704416b639ff8aa7571ef999122a0259bfaf1bbf822345505b11c" +
                "_0x2d29bfbec903479fe4ba991918bab99b494f2bef";

        SignerService signer = new SignerService(WEB3J_CLIENT, chainId, credentials);

        final String token = signer.signEIP712EntityAndBuildToken(eip712Challenge);
        assertThat(token)
                .isNotEmpty()
                .isEqualTo(expectedToken);
    }
    // endregion


    String createTempWallet() throws Exception {
        String walletFilename = WalletUtils.generateFullNewWalletFile(WALLET_PASS, tempDir);
        return tempDir + "/" + walletFilename;
    }
}
