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

package com.iexec.commons.poco.utils;

import com.iexec.commons.poco.security.Signature;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SignatureException;

import static com.iexec.commons.poco.utils.BytesUtils.isNonZeroedHexStringWithPrefixAndProperBytesSize;
import static com.iexec.commons.poco.utils.BytesUtils.stringToBytes;

@Slf4j
public class SignatureUtils {

    public static final Signature EMPTY_SIGNATURE = new Signature();

    private SignatureUtils() {
        throw new UnsupportedOperationException();
    }

    public static boolean isSignature(String hexString) {
        return isNonZeroedHexStringWithPrefixAndProperBytesSize(hexString, 65); //32 + 32 + 1
    }

    public static boolean doesSignatureMatchesAddress(byte[] signatureR,
                                                      byte[] signatureS,
                                                      String hashToCheck,
                                                      String signerAddress) {
        // check that the public address of the signer can be found
        for (int i = 0; i < 4; i++) {
            BigInteger publicKey = Sign.recoverFromSignature((byte) i,
                    new ECDSASignature(
                            new BigInteger(1, signatureR),
                            new BigInteger(1, signatureS)),
                    BytesUtils.stringToBytes(hashToCheck));

            if (publicKey != null) {
                String addressRecovered = "0x" + Keys.getAddress(publicKey);

                if (addressRecovered.equalsIgnoreCase(signerAddress)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isSignatureValid(byte[] message, Signature sign, String signerAddress) {
        BigInteger publicKey;
        Sign.SignatureData signatureData = new Sign.SignatureData(sign.getV(), sign.getR(), sign.getS());

        try {
            publicKey = Sign.signedPrefixedMessageToKey(message, signatureData);
        } catch (SignatureException e) {
            log.error("Failed to recover public key from signature", e);
            return false;
        }

        if (publicKey == null) return false;

        String addressRecovered = "0x" + Keys.getAddress(publicKey);
        return addressRecovered.equalsIgnoreCase(signerAddress);
    }

    /**
     * @deprecated Use {@link #hashAndSign(String, ECKeyPair)}
     */
    @Deprecated(forRemoval = true)
    public static Signature hashAndSign(String stringToSign, String walletAddress, ECKeyPair ecKeyPair) {
        return hashAndSign(stringToSign, ecKeyPair);
    }

    public static Signature hashAndSign(String stringToSign, ECKeyPair ecKeyPair) {
        byte[] message = Hash.sha3(BytesUtils.stringToBytes(stringToSign));
        Sign.SignatureData sign = Sign.signMessage(message, ecKeyPair, false);
        return new Signature(sign.getR(), sign.getS(), sign.getV());
    }

    public static String signAsString(String stringToSign, ECKeyPair ecKeyPair) {
        byte[] message = Numeric.hexStringToByteArray(stringToSign);
        Sign.SignatureData sign = Sign.signMessage(message, ecKeyPair, false);
        return createStringFromSignature(sign);
    }

    private static String createStringFromSignature(Sign.SignatureData sign) {
        String r = Numeric.toHexString(sign.getR());
        String s = Numeric.toHexString(sign.getS());
        String v = Numeric.toHexString(sign.getV());
        return String.join("", r, Numeric.cleanHexPrefix(s), Numeric.cleanHexPrefix(v));
    }

    /**
     * Sign a message prefixed with {@code "\x19Ethereum Signed Message:\n" + len(message)}.
     *
     * @param messageHash Hashed data to sign
     * @param ecKeyPair   Key pair to sign the message
     * @return The signature data
     * @see <a href="https://eips.ethereum.org/EIPS/eip-191">ERC-191</a> specification.
     */
    private static Sign.SignatureData signMessageHash(String messageHash, ECKeyPair ecKeyPair) {
        return Sign.signPrefixedMessage(stringToBytes(messageHash), ecKeyPair);
    }

    /**
     * Sign a message prefixed with {@code "\x19Ethereum Signed Message:\n" + len(message)}.
     *
     * @param messageHash Hashed data to sign
     * @param ecKeyPair   Key pair to sign the message
     * @return The signature data wrapped in a {@link Signature} instance
     */
    public static Signature signMessageHashAndGetSignature(String messageHash, ECKeyPair ecKeyPair) {
        return new Signature(signMessageHash(messageHash, ecKeyPair));
    }

    /**
     * Sign a message prefixed with {@code "\x19Ethereum Signed Message:\n" + len(message)}.
     *
     * @param messageHash Hashed data to sign
     * @param privateKey  Private key to use, an {@code ECKeyPair} will be created from this key
     * @return The signature data wrapper in a {@link Signature} instance
     */
    public static Signature signMessageHashAndGetSignature(String messageHash, String privateKey) {
        final ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.hexStringToByteArray(privateKey));
        return signMessageHashAndGetSignature(messageHash, ecKeyPair);
    }

    /*
     * web3j signedMessageHashToSignerAddress(..) accepts Sign.SignatureData [built on EthereumMessageHash]
     * */
    public static String signedMessageHashToSignerAddress(String messageHash, Sign.SignatureData signatureData) {
        String signerAddress = "";
        try {
            BigInteger publicKey = Sign.signedPrefixedMessageToKey(stringToBytes(messageHash), signatureData);
            signerAddress = Numeric.prependHexPrefix(Keys.getAddress(publicKey));
        } catch (SignatureException e) {
            log.error("Failed to recover public key from signature", e);
        }
        return signerAddress;
    }

    /*
     * iExec signedMessageHashToSignerAddress(..) accepts Signature [built on EthereumMessageHash]
     * */
    public static String signedMessageHashToSignerAddress(String messageHash, Signature signature) {
        Sign.SignatureData signatureData = new Sign.SignatureData(signature.getV()[0], signature.getR(), signature.getS());
        return signedMessageHashToSignerAddress(messageHash, signatureData);
    }

    /*
     * iExec isExpectedSignerOnSignedMessageHash(..) accepts Signature [built on EthereumMessageHash]
     * */
    public static boolean isExpectedSignerOnSignedMessageHash(String messageHash, Signature signature, String expectedSigner) {
        String signerAddress = signedMessageHashToSignerAddress(messageHash, signature);
        return signerAddress.equalsIgnoreCase(expectedSigner);
    }

}
