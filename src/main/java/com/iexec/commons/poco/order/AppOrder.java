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
@JsonDeserialize(builder = AppOrder.AppOrderBuilder.class)
public class AppOrder extends Order {

    String app;
    BigInteger appprice;
    String datasetrestrict;
    String workerpoolrestrict;
    String requesterrestrict;

    @Builder
    AppOrder(
            String app,
            BigInteger appprice,
            BigInteger volume,
            String tag,
            String datasetrestrict,
            String workerpoolrestrict,
            String requesterrestrict,
            String salt,
            String sign) {
        super(volume, tag, salt, sign);
        this.app = app;
        this.appprice = appprice;
        this.datasetrestrict = toLowerCase(datasetrestrict);
        this.workerpoolrestrict = toLowerCase(workerpoolrestrict);
        this.requesterrestrict = toLowerCase(requesterrestrict);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class AppOrderBuilder {
    }

    @Override
    public AppOrder withSignature(String signature) {
        return new AppOrder(
                this.app, this.appprice,
                this.volume, this.tag,
                this.datasetrestrict, this.workerpoolrestrict, this.requesterrestrict,
                this.salt, signature
        );
    }

    // region EIP-712
    public String computeMessageHash() {
        final String type = "AppOrder(address app,uint256 appprice,uint256 volume,bytes32 tag,address datasetrestrict,address workerpoolrestrict,address requesterrestrict,bytes32 salt)";
        final String[] encodedValues = Stream.of(type, app, appprice, volume, tag, datasetrestrict, workerpoolrestrict, requesterrestrict, salt)
                .map(EIP712Utils::encodeData)
                .toArray(String[]::new);
        if (log.isDebugEnabled()) {
            log.debug("{}", type);
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
    public IexecHubContract.AppOrder toHubContract() {
        return new IexecHubContract.AppOrder(
                this.app,
                this.appprice,
                this.volume,
                Numeric.hexStringToByteArray(this.tag),
                this.datasetrestrict,
                this.workerpoolrestrict,
                this.requesterrestrict,
                Numeric.hexStringToByteArray(this.salt),
                Numeric.hexStringToByteArray(this.sign)
        );
    }

    public String toString() {
        return "AppOrder{app=" + app + ", appprice=" + appprice
                + ", volume=" + volume + ", tag=" + tag
                + ", datasetrestrict=" + datasetrestrict
                + ", workerpoolrestrict=" + workerpoolrestrict
                + ", requesterrestrict=" + requesterrestrict
                + ", salt=" + salt + ", sign=" + sign + "}";
    }
}
