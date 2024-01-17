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
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EIP712DatasetOrderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EIP712Domain DOMAIN = new EIP712Domain(133, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
    private final DatasetOrder DATASET_ORDER = DatasetOrder.builder()
            .dataset("0x2550E5B60f48742aBce2275F34417e7cBf5AcA86")
            .datasetprice(BigInteger.valueOf(0))
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
                .isEqualTo("0x94661cab25380e7a6e1c20762988f6f854c5123a17ad27c65580d7c3edcfa2025a9d255c679c4cf7d489560917c17d3af3da83737b3722824918d39aecfedf711c");
    }

    @SneakyThrows
    private ECKeyPair getWallet() {
        return WalletUtils
                .loadCredentials("whatever", "./src/test/resources/wallet.json")
                .getEcKeyPair();
    }

}
