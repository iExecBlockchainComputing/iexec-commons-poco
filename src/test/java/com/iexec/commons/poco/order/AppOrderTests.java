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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

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
    void shouldCastToHubContract() {
        AppOrder appOrder = AppOrder.builder()
                .app("0x1")
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.STANDARD.getValue())
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .sign("0x0")
                .build();
        IexecHubContract.AppOrder web3jAppOrder = appOrder.toHubContract();
        assertThat(web3jAppOrder.app).isEqualTo(appOrder.getApp());
        assertThat(web3jAppOrder.appprice).isEqualTo(appOrder.getAppprice());
        assertThat(web3jAppOrder.volume).isEqualTo(appOrder.getVolume());
        assertThat(web3jAppOrder.tag).isEqualTo(Numeric.hexStringToByteArray(appOrder.getTag()));
        assertThat(web3jAppOrder.datasetrestrict).isEqualTo(appOrder.getDatasetrestrict());
        assertThat(web3jAppOrder.workerpoolrestrict).isEqualTo(appOrder.getWorkerpoolrestrict());
        assertThat(web3jAppOrder.requesterrestrict).isEqualTo(appOrder.getRequesterrestrict());
        assertThat(web3jAppOrder.salt).isEqualTo(Numeric.hexStringToByteArray(appOrder.getSalt()));
        assertThat(appOrder).hasToString(
                "AppOrder{app=0x1, appprice=0"
                        + ", volume=1, tag=0x0000000000000000000000000000000000000000000000000000000000000000"
                        + ", datasetrestrict=0x0000000000000000000000000000000000000000"
                        + ", workerpoolrestrict=0x0000000000000000000000000000000000000000"
                        + ", requesterrestrict=0x0000000000000000000000000000000000000000"
                        + ", salt=" + appOrder.getSalt() + ", sign=0x0}"
        );
    }

    @Test
    void shouldSignAppOrder() throws Exception {
        final int chainId = 133;
        final SignerService signer = new SignerService(null, chainId, "whatever", "./src/test/resources/wallet.json");
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
