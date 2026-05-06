/*
 * Copyright 2023-2026 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class AppOrderTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        AppOrder appOrder = AppOrder.builder().build();
        String jsonString = mapper.writeValueAsString(appOrder);
        assertThat(jsonString).isEqualTo("{\"volume\":null,\"tag\":null,\"salt\":null,\"sign\":null," +
                "\"app\":null,\"appprice\":null,\"datasetrestrict\":\"\",\"workerpoolrestrict\":\"\",\"requesterrestrict\":\"\"}");
        AppOrder parsedAppOrder = mapper.readValue(jsonString, AppOrder.class);
        assertThat(parsedAppOrder).usingRecursiveComparison().isEqualTo(appOrder);
        assertThat(appOrder).hasToString(
                "AppOrder{app=null, appprice=null, volume=null, tag=null"
                        + ", datasetrestrict=, workerpoolrestrict=, requesterrestrict=, salt=null, sign=null}"
        );
    }

    @Test
    void shouldSignAppOrder() throws Exception {
        final int chainId = 133;
        final String walletPath = getClass().getClassLoader().getResource("wallet.json").getPath();
        final SignerService signer = new SignerService(null, chainId, "whatever", walletPath);
        final AppOrder appOrder = AppOrder.builder()
                .app("0x2EbD509d777B187E8394566bA6ec093B9dd73DF1")
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag("0x0000000000000000000000000000000000000000000000000000000000000000")
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt("0xbe858b0eee90cf2e85297bd3df81373f6b4de20c67a3e1f5db1a9d5be8abc3c4")
                .build();
        final EIP712Domain domain = new EIP712Domain(chainId, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
        final AppOrder signedOrder = (AppOrder) signer.signOrderForDomain(appOrder, domain);
        assertThat(signedOrder.getSign()).isEqualTo("0x82c2d8a5f59f1088eb0b9a627c367ae7dae1772c8bd98c394699ae24830611e1171026f4e28d2c60302c34a04c60c4fc2f1363e165072dca04a9f203734978671c");
    }

}
