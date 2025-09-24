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

package com.iexec.commons.poco.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.utils.BytesUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

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
    void shouldCastToHubContract() {
        WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
                .workerpool("0x1")
                .workerpoolprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.STANDARD.getValue())
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .sign("0x0")
                .build();
        IexecHubContract.WorkerpoolOrder web3jWorkerpoolOrder = workerpoolOrder.toHubContract();
        assertThat(web3jWorkerpoolOrder.workerpool).isEqualTo(workerpoolOrder.getWorkerpool());
        assertThat(web3jWorkerpoolOrder.workerpoolprice).isEqualTo(workerpoolOrder.getWorkerpoolprice());
        assertThat(web3jWorkerpoolOrder.volume).isEqualTo(workerpoolOrder.getVolume());
        assertThat(web3jWorkerpoolOrder.tag).isEqualTo(Numeric.hexStringToByteArray(workerpoolOrder.getTag()));
        assertThat(web3jWorkerpoolOrder.category).isEqualTo(workerpoolOrder.getCategory());
        assertThat(web3jWorkerpoolOrder.trust).isEqualTo(workerpoolOrder.getTrust());
        assertThat(web3jWorkerpoolOrder.apprestrict).isEqualTo(workerpoolOrder.getApprestrict());
        assertThat(web3jWorkerpoolOrder.datasetrestrict).isEqualTo(workerpoolOrder.getDatasetrestrict());
        assertThat(web3jWorkerpoolOrder.requesterrestrict).isEqualTo(workerpoolOrder.getRequesterrestrict());
        assertThat(web3jWorkerpoolOrder.salt).isEqualTo(Numeric.hexStringToByteArray(workerpoolOrder.getSalt()));
        assertThat(workerpoolOrder).hasToString(
                "WorkerpoolOrder{workerpool=0x1, workerpoolprice=0"
                        + ", volume=1, tag=0x0000000000000000000000000000000000000000000000000000000000000000"
                        + ", category=0, trust=1"
                        + ", apprestrict=0x0000000000000000000000000000000000000000"
                        + ", datasetrestrict=0x0000000000000000000000000000000000000000"
                        + ", requesterrestrict=0x0000000000000000000000000000000000000000"
                        + ", salt=" + workerpoolOrder.getSalt() + ", sign=0x0}"
        );
    }

    @Test
    void shouldSignWorkerpoolOrder() throws Exception {
        final int chainId = 133;
        final SignerService signer = new SignerService(null, chainId, "whatever", "./src/test/resources/wallet.json");
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
