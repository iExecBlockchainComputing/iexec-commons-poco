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

class RequestOrderTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        RequestOrder requestOrder = RequestOrder.builder().build();
        String jsonString = mapper.writeValueAsString(requestOrder);
        assertThat(jsonString).isEqualTo("{\"volume\":null,\"tag\":null,\"salt\":null,\"sign\":null," +
                "\"app\":\"\",\"appmaxprice\":null,\"dataset\":\"\",\"datasetmaxprice\":null,\"workerpool\":\"\",\"workerpoolmaxprice\":null," +
                "\"requester\":\"\",\"category\":null,\"trust\":null,\"beneficiary\":\"\",\"callback\":\"\",\"params\":null}");
        RequestOrder parsedRequestOrder = mapper.readValue(jsonString, RequestOrder.class);
        assertThat(parsedRequestOrder).usingRecursiveComparison().isEqualTo(requestOrder);
    }

    @Test
    void shouldCastToHubContract() {
        RequestOrder requestOrder = RequestOrder.builder()
                .app("0x1")
                .appmaxprice(BigInteger.TEN)
                .dataset("0x2")
                .datasetmaxprice(BigInteger.TEN)
                .workerpool("0x3")
                .workerpoolmaxprice(BigInteger.TEN)
                .requester("0x4")
                .volume(BigInteger.ONE)
                .tag(OrderTag.STANDARD.getValue())
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .beneficiary("0x5")
                .callback(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .sign("0x0")
                .build();
        IexecHubContract.RequestOrder web3jRequestOrder = requestOrder.toHubContract();
        assertThat(web3jRequestOrder.app).isEqualTo(requestOrder.getApp());
        assertThat(web3jRequestOrder.appmaxprice).isEqualTo(requestOrder.getAppmaxprice());
        assertThat(web3jRequestOrder.dataset).isEqualTo(requestOrder.getDataset());
        assertThat(web3jRequestOrder.datasetmaxprice).isEqualTo(requestOrder.getDatasetmaxprice());
        assertThat(web3jRequestOrder.workerpool).isEqualTo(requestOrder.getWorkerpool());
        assertThat(web3jRequestOrder.workerpoolmaxprice).isEqualTo(requestOrder.getWorkerpoolmaxprice());
        assertThat(web3jRequestOrder.requester).isEqualTo(requestOrder.getRequester());
        assertThat(web3jRequestOrder.volume).isEqualTo(requestOrder.getVolume());
        assertThat(web3jRequestOrder.tag).isEqualTo(Numeric.hexStringToByteArray(requestOrder.getTag()));
        assertThat(web3jRequestOrder.category).isEqualTo(requestOrder.getCategory());
        assertThat(web3jRequestOrder.trust).isEqualTo(requestOrder.getTrust());
        assertThat(web3jRequestOrder.beneficiary).isEqualTo(requestOrder.getBeneficiary());
        assertThat(web3jRequestOrder.callback).isEqualTo(requestOrder.getCallback());
        assertThat(web3jRequestOrder.params).isEqualTo(requestOrder.getParams());
        assertThat(web3jRequestOrder.salt).isEqualTo(Numeric.hexStringToByteArray(requestOrder.getSalt()));
    }

}
