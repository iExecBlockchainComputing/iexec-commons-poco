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
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class WorkerpoolOrderTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder().build();
        String jsonString = mapper.writeValueAsString(workerpoolOrder);
        assertThat(jsonString).isEqualTo("{\"volume\":null,\"tag\":null,\"salt\":null,\"sign\":null," +
                "\"workerpool\":null,\"workerpoolprice\":null,\"trust\":null,\"category\":null,\"apprestrict\":\"\",\"datasetrestrict\":\"\",\"requesterrestrict\":\"\"}");
        WorkerpoolOrder parsedWorkerpoolOrder = mapper.readValue(jsonString, WorkerpoolOrder.class);
        assertThat(parsedWorkerpoolOrder).usingRecursiveComparison().isEqualTo(workerpoolOrder);
        assertThat(workerpoolOrder).hasToString(
                "WorkerpoolOrder{workerpool=null, workerpoolprice=null, volume=null, tag=null"
                        + ", category=null, trust=null"
                        + ", apprestrict=, datasetrestrict=, requesterrestrict=, salt=null, sign=null}"
        );
    }

    @Test
    void shouldSignWorkerpoolOrder() throws Exception {
        final int chainId = 133;
        final String walletPath = getClass().getClassLoader().getResource("wallet.json").getPath();
        final SignerService signer = new SignerService(null, chainId, "whatever", walletPath);
        final WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
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
        final EIP712Domain domain = new EIP712Domain(chainId, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
        final WorkerpoolOrder signedOrder = (WorkerpoolOrder) signer.signOrderForDomain(workerpoolOrder, domain);
        assertThat(signedOrder.getSign()).isEqualTo("0x18bb5dbf608ade315c9e81f0b89929a93aa36aee0a1d51e9119c66799af126596c6cfd1e676ea394e346c616710a675388d5b270a195e494e75d107c87a45dce1c");
    }

}
