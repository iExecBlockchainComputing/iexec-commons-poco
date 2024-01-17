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
import com.iexec.commons.poco.order.AppOrder;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EIP712AppOrderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EIP712Domain DOMAIN = new EIP712Domain(133, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
    private final AppOrder APP_ORDER = AppOrder.builder()
            .app("0x2EbD509d777B187E8394566bA6ec093B9dd73DF1")
            .appprice(BigInteger.ZERO)
            .volume(BigInteger.ONE)
            .tag("0x0000000000000000000000000000000000000000000000000000000000000000")
            .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
            .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
            .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
            .salt("0xbe858b0eee90cf2e85297bd3df81373f6b4de20c67a3e1f5db1a9d5be8abc3c4")
            .build();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        EIP712AppOrder eip712AppOrder = new EIP712AppOrder(DOMAIN, APP_ORDER);
        String jsonString = mapper.writeValueAsString(eip712AppOrder);
        EIP712AppOrder deserializedEip712AppOrder = mapper.readValue(jsonString, EIP712AppOrder.class);
        assertThat(deserializedEip712AppOrder).usingRecursiveComparison().isEqualTo(eip712AppOrder);
    }

    /**
     * Expected signature string could also be found with:
     * <p>
     * iexec order sign --app --chain 133 \
     * --keystoredir /home/$USER/iexecdev/iexec-common/src/test/resources/ \
     * --wallet-file wallet.json --password whatever
     * <p>
     * Note: Don't forget to update salt
     */
    @Test
    void signAppOrderEIP712() {
        EIP712AppOrder eip712AppOrder = new EIP712AppOrder(DOMAIN, APP_ORDER);
        String signatureString = eip712AppOrder.signMessage(getWallet());
        assertThat(signatureString)
                .isEqualTo("0x82c2d8a5f59f1088eb0b9a627c367ae7dae1772c8bd98c394699ae24830611e1171026f4e28d2c60302c34a04c60c4fc2f1363e165072dca04a9f203734978671c");
    }

    @SneakyThrows
    private ECKeyPair getWallet() {
        return WalletUtils
                .loadCredentials("whatever", "./src/test/resources/wallet.json")
                .getEcKeyPair();
    }

}
