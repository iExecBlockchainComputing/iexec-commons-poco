/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.eip712.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.order.WorkerpoolOrder;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EIP712WorkerpoolOrderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EIP712Domain DOMAIN = new EIP712Domain(133, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
    private final WorkerpoolOrder WORKERPOOL_ORDER = WorkerpoolOrder.builder()
            .workerpool("0x53Ef1328a96E40E125bca15b9a4da045C5e63E1A")
            .workerpoolprice(BigInteger.ZERO)
            .volume(BigInteger.ONE)
            .tag("0x0000000000000000000000000000000000000000000000000000000000000000")
            .category(BigInteger.ZERO)
            .trust(BigInteger.ZERO)
            .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
            .apprestrict(BytesUtils.EMPTY_ADDRESS)
            .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
            .salt("0x40af1a4975ca6ca7285d7738e51c8da91a9daee4a23fb45d105068be56f85e56")
            .build();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        EIP712WorkerpoolOrder eip712WorkerpoolOrder = new EIP712WorkerpoolOrder(DOMAIN, WORKERPOOL_ORDER);
        String jsonString = mapper.writeValueAsString(eip712WorkerpoolOrder);
        EIP712WorkerpoolOrder deserializedEip712WorkerPoolOrder = mapper.readValue(jsonString, EIP712WorkerpoolOrder.class);
        assertThat(deserializedEip712WorkerPoolOrder).usingRecursiveComparison().isEqualTo(eip712WorkerpoolOrder);
    }

    /**
     * Expected signature string could also be found with:
     * <p>
     * iexec order sign --workerpool --chain 133 \
     * --keystoredir /home/$USER/iexecdev/iexec-common/src/test/resources/ \
     * --wallet-file wallet.json --password whatever
     * <p>
     * Note: Don't forget to update salt
     */
    @Test
    void signWorkerpoolOrderEIP712() {
        EIP712WorkerpoolOrder eip712WorkerpoolOrder = new EIP712WorkerpoolOrder(DOMAIN, WORKERPOOL_ORDER);
        String signatureString = eip712WorkerpoolOrder.signMessage(getWallet());
        assertThat(signatureString)
                .isEqualTo("0x18bb5dbf608ade315c9e81f0b89929a93aa36aee0a1d51e9119c66799af126596c6cfd1e676ea394e346c616710a675388d5b270a195e494e75d107c87a45dce1c");
    }

    @SneakyThrows
    private ECKeyPair getWallet() {
        return WalletUtils
                .loadCredentials("whatever", "./src/test/resources/wallet.json")
                .getEcKeyPair();
    }

}
