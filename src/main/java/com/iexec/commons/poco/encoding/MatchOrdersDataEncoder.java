/*
 * Copyright 2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.encoding;

import com.iexec.commons.poco.order.AppOrder;
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.order.RequestOrder;
import com.iexec.commons.poco.order.WorkerpoolOrder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static com.iexec.commons.poco.encoding.Utils.toHexString;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MatchOrdersDataEncoder {

    private static final String ASSERT_DATASET_DEAL_COMPATIBILITY_SELECTOR = "0x80f03425";
    private static final String MATCH_ORDERS_SELECTOR = "0x156194d4";

    public static String encodeAssertDatasetDealCompatibility(final DatasetOrder datasetOrder, final String dealId) {
        final StringBuilder sb = new StringBuilder(ASSERT_DATASET_DEAL_COMPATIBILITY_SELECTOR);
        long offset = 2;
        String datasetOrderContribOffset = toHexString(BigInteger.valueOf(offset * 32));
        String datasetOrderContrib = createDatasetOrderTxData(datasetOrder);

        sb.append(datasetOrderContribOffset);
        sb.append(toHexString(dealId));
        sb.append(datasetOrderContrib);

        return sb.toString();
    }

    /**
     * Encodes a {@code BrokerOrder} to a hexadecimal string. This string is the payload representing
     * a {@code matchOrders} call. This call is submitted to the blockchain network by providing the payload
     * as the {@code input} parameter of a {@code eth_sendRawTransaction} JSON RPC call.
     *
     * @param appOrder The {@code AppOrder} values to encode.
     * @return The payload to pass to a {@code eth_sendRawTransaction} RPC call {@code input} parameter.
     */
    public static String encode(AppOrder appOrder, DatasetOrder datasetOrder, WorkerpoolOrder workerpoolOrder, RequestOrder requestOrder) {
        StringBuilder sb = new StringBuilder(MATCH_ORDERS_SELECTOR);
        long offset = 4;
        String appOrderContribOffset = toHexString(BigInteger.valueOf(offset * 32));
        // app order contrib and offset
        String appOrderContrib = createAppOrderTxData(appOrder);
        offset += appOrderContrib.length() / 64;
        String datasetOrderContribOffset = toHexString(BigInteger.valueOf(offset * 32));
        // dataset order contrib and offset
        String datasetOrderContrib = createDatasetOrderTxData(datasetOrder);
        offset += datasetOrderContrib.length() / 64;
        String workerpoolOrderContribOffset = toHexString(BigInteger.valueOf(offset * 32));
        // workerpool order contrib and offset
        String workerpoolOrderContrib = createWorkerpoolOrderTxData(workerpoolOrder);
        offset += workerpoolOrderContrib.length() / 64;
        String requestOrderContribOffset = toHexString(BigInteger.valueOf(offset * 32));
        // request order contrib and offset
        String requestOrderContrib = createRequestOrderTxData(requestOrder);

        // append offsets to contributions
        sb.append(appOrderContribOffset);
        sb.append(datasetOrderContribOffset);
        sb.append(workerpoolOrderContribOffset);
        sb.append(requestOrderContribOffset);
        // append contributions
        sb.append(appOrderContrib);
        sb.append(datasetOrderContrib);
        sb.append(workerpoolOrderContrib);
        sb.append(requestOrderContrib);

        log.debug(sb.toString());
        return sb.toString();
    }

    /**
     * Encodes the {@code AppOrder} part of the {@code BrokerOrder}.
     *
     * @param appOrder The {@code AppOrder} to encode.
     * @return The {@code AppOrder} encoded part of the payload.
     */
    static String createAppOrderTxData(AppOrder appOrder) {
        long offset = 9;
        return toHexString(appOrder.getApp()) +
                toHexString(appOrder.getAppprice()) +
                toHexString(appOrder.getVolume()) +
                toHexString(appOrder.getTag()) +
                toHexString(appOrder.getDatasetrestrict()) +
                toHexString(appOrder.getWorkerpoolrestrict()) +
                toHexString(appOrder.getRequesterrestrict()) +
                toHexString(appOrder.getSalt()) +
                toHexString(BigInteger.valueOf(offset * 32)) +
                TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(appOrder.getSign())));
    }

    /**
     * Encodes the {@code DatasetOrder} part of the {@code BrokerOrder}.
     *
     * @param datasetOrder The {@code DatasetOrder} to encode.
     * @return The {@code DatasetOrder} encoded part of the payload.
     */
    static String createDatasetOrderTxData(DatasetOrder datasetOrder) {
        long offset = 9;
        return toHexString(datasetOrder.getDataset()) +
                toHexString(datasetOrder.getDatasetprice()) +
                toHexString(datasetOrder.getVolume()) +
                toHexString(datasetOrder.getTag()) +
                toHexString(datasetOrder.getApprestrict()) +
                toHexString(datasetOrder.getWorkerpoolrestrict()) +
                toHexString(datasetOrder.getRequesterrestrict()) +
                toHexString(datasetOrder.getSalt()) +
                toHexString(BigInteger.valueOf(offset * 32)) +
                TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(datasetOrder.getSign())));
    }

    /**
     * Encodes the {@code WorkerpoolOrder} part of the {@code BrokerOrder}.
     *
     * @param workerpoolOrder The {@code WorkerpoolOrder} to encode.
     * @return The {@code WorkerpoolOrder} encoded part of the payload.
     */
    static String createWorkerpoolOrderTxData(WorkerpoolOrder workerpoolOrder) {
        long offset = 11;
        return toHexString(Numeric.toBigInt(workerpoolOrder.getWorkerpool())) +
                toHexString(workerpoolOrder.getWorkerpoolprice()) +
                toHexString(workerpoolOrder.getVolume()) +
                toHexString(workerpoolOrder.getTag()) +
                toHexString(workerpoolOrder.getCategory()) +
                toHexString(workerpoolOrder.getTrust()) +
                toHexString(workerpoolOrder.getApprestrict()) +
                toHexString(workerpoolOrder.getDatasetrestrict()) +
                toHexString(workerpoolOrder.getRequesterrestrict()) +
                toHexString(workerpoolOrder.getSalt()) +
                toHexString(BigInteger.valueOf(offset * 32)) +
                TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(workerpoolOrder.getSign())));
    }

    /**
     * Encodes the {@code RequestOrder} part of the {@code BrokerOrder}.
     *
     * @param requestOrder The {@code RequestOrder} to encode.
     * @return The {@code RequestOrder} encoded part of the payload.
     */
    static String createRequestOrderTxData(RequestOrder requestOrder) {
        long offset = 16;

        StringBuilder sb = new StringBuilder();
        sb.append(toHexString(requestOrder.getApp()));
        sb.append(toHexString(requestOrder.getAppmaxprice()));
        sb.append(toHexString(requestOrder.getDataset()));
        sb.append(toHexString(requestOrder.getDatasetmaxprice()));
        sb.append(toHexString(requestOrder.getWorkerpool()));
        sb.append(toHexString(requestOrder.getWorkerpoolmaxprice()));
        sb.append(toHexString(requestOrder.getRequester()));
        sb.append(toHexString(requestOrder.getVolume()));
        sb.append(toHexString(requestOrder.getTag()));
        sb.append(toHexString(requestOrder.getCategory()));
        sb.append(toHexString(requestOrder.getTrust()));
        sb.append(toHexString(requestOrder.getBeneficiary()));
        sb.append(toHexString(requestOrder.getCallback()));
        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        sb.append(toHexString(requestOrder.getSalt()));

        String paramsContrib = TypeEncoder.encode(new DynamicBytes(requestOrder.getParams().getBytes(StandardCharsets.UTF_8)));
        offset += paramsContrib.length() / 64;

        sb.append(toHexString(BigInteger.valueOf(offset * 32)));
        sb.append(paramsContrib);
        sb.append(TypeEncoder.encode(new DynamicBytes(Numeric.hexStringToByteArray(requestOrder.getSign()))));
        return sb.toString();
    }

}
