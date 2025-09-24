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
@JsonDeserialize(builder = WorkerpoolOrder.WorkerpoolOrderBuilder.class)
public class WorkerpoolOrder extends Order {

    private static final String EIP712_TYPE = "WorkerpoolOrder(address workerpool,uint256 workerpoolprice,uint256 volume,bytes32 tag,uint256 category,uint256 trust,address apprestrict,address datasetrestrict,address requesterrestrict,bytes32 salt)";

    String workerpool;
    BigInteger workerpoolprice;
    BigInteger trust;
    BigInteger category;
    String apprestrict;
    String datasetrestrict;
    String requesterrestrict;

    @Builder
    WorkerpoolOrder(
            String workerpool,
            BigInteger workerpoolprice,
            BigInteger volume,
            String tag,
            BigInteger category,
            BigInteger trust,
            String apprestrict,
            String datasetrestrict,
            String requesterrestrict,
            String salt,
            String sign) {
        super(volume, tag, salt, sign);
        this.workerpool = workerpool;
        this.workerpoolprice = workerpoolprice;
        this.category = category;
        this.trust = trust;
        this.apprestrict = toLowerCase(apprestrict);
        this.datasetrestrict = toLowerCase(datasetrestrict);
        this.requesterrestrict = toLowerCase(requesterrestrict);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class WorkerpoolOrderBuilder {
    }

    @Override
    public WorkerpoolOrder withSignature(String signature) {
        return new WorkerpoolOrder(
                this.workerpool, this.workerpoolprice,
                this.volume, this.tag, this.category, this.trust,
                this.apprestrict, this.datasetrestrict, this.requesterrestrict,
                this.salt, signature
        );
    }

    // region EIP-712
    public String computeMessageHash() {
        final String[] encodedValues = Stream.of(EIP712_TYPE, workerpool, workerpoolprice, volume, tag, category, trust, apprestrict, datasetrestrict, requesterrestrict, salt)
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
    public IexecHubContract.WorkerpoolOrder toHubContract() {
        return new IexecHubContract.WorkerpoolOrder(
                this.workerpool,
                this.workerpoolprice,
                this.volume,
                Numeric.hexStringToByteArray(this.tag),
                this.category,
                this.trust,
                this.apprestrict,
                this.datasetrestrict,
                this.requesterrestrict,
                Numeric.hexStringToByteArray(this.salt),
                Numeric.hexStringToByteArray(this.sign)
        );
    }

    public String toString() {
        return "WorkerpoolOrder{workerpool=" + workerpool + ", workerpoolprice=" + workerpoolprice
                + ", volume=" + volume + ", tag=" + tag
                + ", category=" + category + ", trust=" + trust
                + ", apprestrict=" + apprestrict
                + ", datasetrestrict=" + datasetrestrict
                + ", requesterrestrict=" + requesterrestrict
                + ", salt=" + salt + ", sign=" + sign + "}";
    }
}
