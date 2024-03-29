/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

@Value
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(builder = RequestOrder.RequestOrderBuilder.class)
public class RequestOrder extends Order {

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
