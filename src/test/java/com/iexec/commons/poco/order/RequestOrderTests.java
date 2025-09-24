/*
 * Copyright 2023-2025 IEXEC BLOCKCHAIN TECH
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
        assertThat(requestOrder).hasToString(
                "RequestOrder{app=, appmaxprice=null"
                        + ", dataset=, datasetmaxprice=null"
                        + ", workerpool=, workerpoolmaxprice=null"
                        + ", requester=, volume=null, tag=null, category=null, trust=null"
                        + ", beneficiary=, callback=, params=null"
                        + ", salt=null, sign=null}"
        );
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
        assertThat(requestOrder).hasToString(
                "RequestOrder{app=0x1, appmaxprice=10"
                        + ", dataset=0x2, datasetmaxprice=10"
                        + ", workerpool=0x3, workerpoolmaxprice=10"
                        + ", requester=0x4, volume=1, tag=0x0000000000000000000000000000000000000000000000000000000000000000"
                        + ", category=0, trust=1, beneficiary=0x5"
                        + ", callback=0x0000000000000000000000000000000000000000, params=null"
                        + ", salt=" + requestOrder.getSalt() + ", sign=0x0}"
        );
    }

    @Test
    void shouldSignRequestOrder() throws Exception {
        final int chainId = 133;
        final SignerService signer = new SignerService(null, chainId, "whatever", "./src/test/resources/wallet.json");
        final RequestOrder requestOrder = RequestOrder.builder()
                .app("0x6709CAe77CDa2cbA8Cb90A4F5a4eFfb5c8Fe8367")
                .appmaxprice(BigInteger.ZERO)
                .dataset(BytesUtils.EMPTY_ADDRESS)
                .datasetmaxprice(BigInteger.ZERO)
                .workerpool("0x506fA5EaCa52B5d2F133452a45FFA68aD1CfB3C5")
                .workerpoolmaxprice(BigInteger.ZERO)
                .requester("0x1ec09e1782a43a770d54e813379c730e0b29ad4b")
                .volume(BigInteger.ONE)
                .tag(BytesUtils.toByte32HexString(0x1)) // any tag here
                .category(BigInteger.ZERO)
                .trust(BigInteger.ZERO)
                .beneficiary(BytesUtils.EMPTY_ADDRESS)
                .callback(BytesUtils.EMPTY_ADDRESS)
                .params("{\"iexec_tee_post_compute_fingerprint\":\"76bfdee97e692b729e989694f3a566cf0e1de95fc456ff5ee88c75b1cb865e33|1eb627c1c94bbca03178b099b13fb4d1|13076027fc67accba753a3ed2edf03227dfd013b450d68833a5589ec44132100\",\"iexec_tee_post_compute_image\":\"iexechub/tee-worker-post-compute:1.0.0\",\"iexec_result_storage_provider\":\"ipfs\",\"iexec_result_storage_proxy\":\"https://result.viviani.iex.ec\",\"iexec_result_encryption\":false,\"iexec_input_files\":[],\"iexec_args\":\"Alice\"}")
                .salt("0xee5c64cd59eaa084f59dbaa8f20b87260c4d6ac35c83214da657681bfe4e7632")
                .build();
        final EIP712Domain domain = new EIP712Domain(chainId, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
        final RequestOrder signedOrder = (RequestOrder) signer.signOrderForDomain(requestOrder, domain);
        assertThat(signedOrder.getSign()).isEqualTo("0x611511fa5169dff40f7b4c0013e9f149e79dfddacd80a19852a1e9b42294eaef4329367f01eb48930f990a418befed0c5634e493809f2e9a6a60727137964df51c");
    }

}
