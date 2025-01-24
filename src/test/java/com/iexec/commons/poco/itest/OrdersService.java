/*
 * Copyright 2024-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.itest;

import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.eip712.EIP712Domain;
import com.iexec.commons.poco.eip712.entity.EIP712AppOrder;
import com.iexec.commons.poco.eip712.entity.EIP712DatasetOrder;
import com.iexec.commons.poco.eip712.entity.EIP712RequestOrder;
import com.iexec.commons.poco.eip712.entity.EIP712WorkerpoolOrder;
import com.iexec.commons.poco.encoding.MatchOrdersDataEncoder;
import com.iexec.commons.poco.order.*;
import com.iexec.commons.poco.utils.BytesUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

import static com.iexec.commons.poco.itest.IexecHubTestService.GAS_PRICE;
import static com.iexec.commons.poco.itest.IexecHubTestService.IEXEC_HUB_ADDRESS;

public class OrdersService {

    static final String APP_NAME = "my-app";
    static final String DATASET_NAME = "my-dataset";
    static final String WORKERPOOL_NAME = "my-workerpool";

    private static final long CHAIN_ID = 65535L;

    private final EIP712Domain domain;
    private final SignerService signerService;

    public OrdersService(SignerService signerService) {
        this.domain = new EIP712Domain(CHAIN_ID, IEXEC_HUB_ADDRESS);
        this.signerService = signerService;
    }

    public AppOrder buildSignedAppOrder(String appAddress) {
        final AppOrder appOrder = AppOrder.builder()
                .app(appAddress)
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
        final String sig = signerService.signEIP712Entity(new EIP712AppOrder(domain, appOrder));
        return appOrder.withSignature(sig);
    }

    public DatasetOrder buildSignedDatasetOrder(String datasetAddress) {
        final DatasetOrder datasetOrder = DatasetOrder.builder()
                .dataset(datasetAddress)
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
        final String sig = signerService.signEIP712Entity(new EIP712DatasetOrder(domain, datasetOrder));
        return datasetOrder.withSignature(sig);
    }

    public WorkerpoolOrder buildSignedWorkerpoolOrder(String workerpoolAddress, BigInteger trust) {
        final WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
                .workerpool(workerpoolAddress)
                .workerpoolprice(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .category(BigInteger.ZERO)
                .trust(trust)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
        final String sig = signerService.signEIP712Entity(new EIP712WorkerpoolOrder(domain, workerpoolOrder));
        return workerpoolOrder.withSignature(sig);
    }

    public RequestOrder buildSignedRequestOrder(AppOrder appOrder, DatasetOrder datasetOrder, WorkerpoolOrder workerpoolOrder, BigInteger trust) {
        final TreeMap<String, String> iexecSecrets = new TreeMap<>(Map.of(
                "1", "first-secret",
                "2", "second-secret",
                "3", "third-secret"));
        final DealParams dealParams = DealParams.builder()
                .iexecResultEncryption(true)
                .iexecResultStorageProvider("ipfs")
                .iexecResultStorageProxy("http://result-proxy:13200")
                .iexecSecrets(iexecSecrets)
                .build();
        final RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(appOrder.getAppprice())
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(datasetOrder.getDatasetprice())
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(workerpoolOrder.getWorkerpoolprice())
                .requester(signerService.getAddress())
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .category(BigInteger.ZERO)
                .trust(trust)
                .beneficiary(signerService.getAddress())
                .callback(BytesUtils.EMPTY_ADDRESS)
                .params(dealParams.toJsonString())
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
        final String sig = signerService.signEIP712Entity(new EIP712RequestOrder(domain, requestOrder));
        return requestOrder.withSignature(sig);
    }

    public DealOrders buildAllSignedOrders(final String appAddress, final String datasetAddress, final String workerpoolAddress, final BigInteger trust) {
        final AppOrder signedAppOrder = buildSignedAppOrder(appAddress);
        final DatasetOrder signedDatasetOrder = buildSignedDatasetOrder(datasetAddress);
        final WorkerpoolOrder signedWorkerpoolOrder = buildSignedWorkerpoolOrder(workerpoolAddress, trust);
        final RequestOrder signedRequestOrder = buildSignedRequestOrder(
                signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, trust);
        return new DealOrders(signedAppOrder, signedDatasetOrder, signedWorkerpoolOrder, signedRequestOrder);
    }

    public String callMatchOrders(final DealOrders orders) throws IOException {
        final String matchOrdersTxData = encodeMathOrdersTxData(orders);
        return signerService.sendCall(IEXEC_HUB_ADDRESS, matchOrdersTxData);
    }

    public String submitMatchOrders(final DealOrders orders, final BigInteger nonce) throws IOException {
        final String matchOrdersTxData = encodeMathOrdersTxData(orders);
        return signerService.signAndSendTransaction(nonce, GAS_PRICE, IEXEC_HUB_ADDRESS, matchOrdersTxData);
    }

    private String encodeMathOrdersTxData(final DealOrders orders) {
        return MatchOrdersDataEncoder.encode(
                orders.appOrder(), orders.datasetOrder(), orders.workerpoolOrder(), orders.requestOrder());
    }

    public record DealOrders(AppOrder appOrder,
                             DatasetOrder datasetOrder,
                             WorkerpoolOrder workerpoolOrder,
                             RequestOrder requestOrder) {
    }
}
