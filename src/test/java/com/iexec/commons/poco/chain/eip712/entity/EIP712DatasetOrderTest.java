/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.chain.eip712.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.chain.eip712.EIP712Domain;
import com.iexec.commons.poco.sdk.order.payload.DatasetOrder;
import com.iexec.commons.poco.utils.BytesUtils;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import java.io.IOException;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EIP712DatasetOrderTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final EIP712Domain DOMAIN = new EIP712Domain(133, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
    private static final DatasetOrder DATASET_ORDER = DatasetOrder.builder()
            .dataset("0x2550E5B60f48742aBce2275F34417e7cBf5AcA86")
            .price(BigInteger.valueOf(0))
            .volume(BigInteger.valueOf(1000000))
            .tag("0x0000000000000000000000000000000000000000000000000000000000000001")
            .apprestrict(BytesUtils.EMPTY_ADDRESS)
            .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
            .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
            .salt("0xc49d07f99c47096900653b6ade4ccde4c52f773a5ad68f1da0a47c993cad4595")
            .build();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        EIP712DatasetOrder eip712DatasetOrder = new EIP712DatasetOrder(DOMAIN, DATASET_ORDER);
        String jsonString = mapper.writeValueAsString(eip712DatasetOrder);
        EIP712DatasetOrder deserializedEip712DatasetOrder = mapper.readValue(jsonString, EIP712DatasetOrder.class);
        assertThat(deserializedEip712DatasetOrder).usingRecursiveComparison().isEqualTo(eip712DatasetOrder);
    }

    /**
     * Expected signature string could also be found with:
     * <p>
     * iexec order sign --dataset --chain 133 \
     * --keystoredir /home/$USER/iexecdev/iexec-common/src/test/resources/ \
     * --wallet-file wallet.json --password whatever
     * <p>
     * Note: Don't forget to update salt
     */
    @Test
    void signDatasetOrderEIP712() {
        EIP712DatasetOrder eip712DatasetOrder = new EIP712DatasetOrder(DOMAIN, DATASET_ORDER);
        String signatureString = eip712DatasetOrder.signMessage(getWallet());
        assertThat(signatureString)
                .isEqualTo("0x955db5242901dfec80d1cf20dce54a8c60274db55fb572ead03f32a2475e18b60e308e1a3bc599d774549283ec737bcedca8420bdae9e4784e3f62e8f4ff085f1c");
    }

    private ECKeyPair getWallet() {
        try {
            return WalletUtils.loadCredentials(
                    "whatever",
                    "./src/test/resources/wallet.json")
                    .getEcKeyPair();
        } catch (IOException | CipherException e) {
            e.printStackTrace();
        }
        return null;
    }

}