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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DatasetOrderTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserialize() throws JsonProcessingException {
        DatasetOrder datasetOrder = DatasetOrder.builder().build();
        String jsonString = mapper.writeValueAsString(datasetOrder);
        assertThat(jsonString).isEqualTo("{\"volume\":null,\"tag\":null,\"salt\":null,\"sign\":null," +
                "\"dataset\":null,\"datasetprice\":null,\"apprestrict\":\"\",\"workerpoolrestrict\":\"\",\"requesterrestrict\":\"\"}");
        DatasetOrder parsedDatasetOrder = mapper.readValue(jsonString, DatasetOrder.class);
        assertThat(parsedDatasetOrder).usingRecursiveComparison().isEqualTo(datasetOrder);
        Assertions.assertThat(datasetOrder).hasToString(
                "DatasetOrder{dataset=null, datasetprice=null, volume=null, tag=null"
                        + ", apprestrict=, workerpoolrestrict=, requesterrestrict=, salt=null, sign=null}"
        );
    }

    @Test
    void shouldCastToHubContract() {
        DatasetOrder datasetOrder = DatasetOrder.builder()
                .dataset("0x1")
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.STANDARD.getValue())
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .sign("0x0")
                .build();
        IexecHubContract.DatasetOrder web3jDatasetOrder = datasetOrder.toHubContract();
        assertThat(web3jDatasetOrder.dataset).isEqualTo(datasetOrder.getDataset());
        assertThat(web3jDatasetOrder.datasetprice).isEqualTo(datasetOrder.getDatasetprice());
        assertThat(web3jDatasetOrder.volume).isEqualTo(datasetOrder.getVolume());
        assertThat(web3jDatasetOrder.tag).isEqualTo(Numeric.hexStringToByteArray(datasetOrder.getTag()));
        assertThat(web3jDatasetOrder.apprestrict).isEqualTo(datasetOrder.getApprestrict());
        assertThat(web3jDatasetOrder.workerpoolrestrict).isEqualTo(datasetOrder.getWorkerpoolrestrict());
        assertThat(web3jDatasetOrder.requesterrestrict).isEqualTo(datasetOrder.getRequesterrestrict());
        assertThat(web3jDatasetOrder.salt).isEqualTo(Numeric.hexStringToByteArray(datasetOrder.getSalt()));
        Assertions.assertThat(datasetOrder).hasToString(
                "DatasetOrder{dataset=0x1, datasetprice=0"
                        + ", volume=1, tag=0x0000000000000000000000000000000000000000000000000000000000000000"
                        + ", apprestrict=0x0000000000000000000000000000000000000000"
                        + ", workerpoolrestrict=0x0000000000000000000000000000000000000000"
                        + ", requesterrestrict=0x0000000000000000000000000000000000000000"
                        + ", salt=" + datasetOrder.getSalt() + ", sign=0x0}"
        );
    }

}
