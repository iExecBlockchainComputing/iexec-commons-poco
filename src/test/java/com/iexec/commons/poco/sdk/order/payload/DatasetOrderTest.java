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

package com.iexec.commons.poco.sdk.order.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iexec.commons.poco.contract.generated.IexecLibOrders_v5;
import com.iexec.commons.poco.utils.BytesUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

class DatasetOrderTest {

    @Test
    void getDatasetOrder() throws JsonProcessingException {
        String tag = BytesUtils.toByte32HexString(0xa);
        DatasetOrder datasetOrder = DatasetOrder.builder()
                .dataset(BytesUtils.EMPTY_ADDRESS)
                .price(BigInteger.valueOf(0))
                .volume(BigInteger.valueOf(1))
                .tag(tag)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(BytesUtils.EMPTY_HEX_STRING_32)
                .build();

        IexecLibOrders_v5.DatasetOrder generatedDatasetOrder = new IexecLibOrders_v5.DatasetOrder(
                BytesUtils.EMPTY_ADDRESS,
                BigInteger.valueOf(0),
                BigInteger.valueOf(1),
                BytesUtils.stringToBytes(tag),
                BytesUtils.EMPTY_ADDRESS,
                BytesUtils.EMPTY_ADDRESS,
                BytesUtils.EMPTY_ADDRESS,
                BytesUtils.stringToBytes(BytesUtils.EMPTY_HEX_STRING_32),
                null);

        Assertions.assertThat(datasetOrder.getDataset())
                .isEqualTo(generatedDatasetOrder.dataset);
        Assertions.assertThat(datasetOrder.getDatasetprice())
                .isEqualTo(generatedDatasetOrder.datasetprice);
        Assertions.assertThat(datasetOrder.getVolume())
                .isEqualTo(generatedDatasetOrder.volume);
        Assertions.assertThat(datasetOrder.getTag())
                .isEqualTo(BytesUtils.bytesToString(generatedDatasetOrder.tag));
        Assertions.assertThat(datasetOrder.getApprestrict())
                .isEqualTo(generatedDatasetOrder.apprestrict);
        Assertions.assertThat(datasetOrder.getWorkerpoolrestrict())
                .isEqualTo(generatedDatasetOrder.workerpoolrestrict);
        Assertions.assertThat(datasetOrder.getRequesterrestrict())
                .isEqualTo(generatedDatasetOrder.requesterrestrict);
        Assertions.assertThat(datasetOrder.getSalt())
                .isEqualTo(BytesUtils.bytesToString(generatedDatasetOrder.salt));
    }
}