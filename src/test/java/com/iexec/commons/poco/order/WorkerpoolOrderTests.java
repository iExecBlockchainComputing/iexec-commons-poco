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
import com.iexec.commons.poco.contract.generated.IexecHubContract;
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
    }

}
