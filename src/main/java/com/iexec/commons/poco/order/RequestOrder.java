/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.eip712.EIP712Utils;
import com.iexec.commons.poco.utils.HashUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.stream.Stream;

@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(builder = RequestOrder.RequestOrderBuilder.class)
public class RequestOrder extends Order {

    private static final String EIP712_TYPE = "RequestOrder(address app,uint256 appmaxprice,address dataset,uint256 datasetmaxprice,address workerpool,uint256 workerpoolmaxprice,address requester,uint256 volume,bytes32 tag,uint256 category,uint256 trust,address beneficiary,address callback,string params,bytes32 salt)";

    String app;
    BigInteger appmaxprice;
    String dataset;
    BigInteger datasetmaxprice;
    String workerpool;
    BigInteger workerpoolmaxprice;
    String requester;
    BigInteger category;
    BigInteger trust;
    String beneficiary;
    String callback;
    String params;

    @Builder
    RequestOrder(
            String app,
            BigInteger appmaxprice,
            String dataset,
            BigInteger datasetmaxprice,
            String workerpool,
            BigInteger workerpoolmaxprice,
            String requester,
            BigInteger volume,
            String tag,
            BigInteger category,
            BigInteger trust,
            String beneficiary,
            String callback,
            String params,
            String salt,
            String sign) {
        super(volume, tag, salt, sign);
        this.app = toLowerCase(app);
        this.appmaxprice = appmaxprice;
        this.dataset = toLowerCase(dataset);
        this.datasetmaxprice = datasetmaxprice;
        this.workerpool = toLowerCase(workerpool);
        this.workerpoolmaxprice = workerpoolmaxprice;
        this.requester = toLowerCase(requester);
        this.category = category;
        this.trust = trust;
        this.beneficiary = toLowerCase(beneficiary);
        this.callback = toLowerCase(callback);
        this.params = params;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class RequestOrderBuilder {
    }

    @Override
    public RequestOrder withSignature(String signature) {
        return new RequestOrder(
                this.app, this.appmaxprice,
                this.dataset, this.datasetmaxprice,
                this.workerpool, this.workerpoolmaxprice,
                this.requester, this.volume, this.tag, this.category,
                this.trust, this.beneficiary, this.callback, this.params, this.salt, signature
        );
    }

    // region EIP-712
    public String computeMessageHash() {
        final String[] encodedValues = Stream.of(EIP712_TYPE, app, appmaxprice, dataset, datasetmaxprice, workerpool, workerpoolmaxprice, requester, volume, tag, category, trust, beneficiary, callback, params, salt)
                .map(EIP712Utils::encodeData)
                .toArray(String[]::new);
        if (log.isDebugEnabled()) {
            log.debug("{}", EIP712_TYPE);
            for (String value : encodedValues) {
                log.debug("{}", value);
            }
        }
        return HashUtils.concatenateAndHash(encodedValues);
    }
    // endregion

    /**
     * @deprecated no more used
     */
    @Deprecated(forRemoval = true)
    public IexecHubContract.RequestOrder toHubContract() {
        return new IexecHubContract.RequestOrder(
                this.app,
                this.appmaxprice,
                this.dataset,
                this.datasetmaxprice,
                this.workerpool,
                this.workerpoolmaxprice,
                this.requester,
                this.volume,
                Numeric.hexStringToByteArray(this.tag),
                this.category,
                this.trust,
                this.beneficiary,
                this.callback,
                this.params,
                Numeric.hexStringToByteArray(this.salt),
                Numeric.hexStringToByteArray(this.sign)
        );
    }

    public String toString() {
        return "RequestOrder{"
                + "app=" + app + ", appmaxprice=" + appmaxprice
                + ", dataset=" + dataset + ", datasetmaxprice=" + datasetmaxprice
                + ", workerpool=" + workerpool + ", workerpoolmaxprice=" + workerpoolmaxprice
                + ", requester=" + requester + ", volume=" + volume + ", tag=" + tag
                + ", category=" + category + ", trust=" + trust
                + ", beneficiary=" + beneficiary + ", callback=" + callback + ", params=" + params
                + ", salt=" + salt + ", sign=" + sign + "}";
    }
}
