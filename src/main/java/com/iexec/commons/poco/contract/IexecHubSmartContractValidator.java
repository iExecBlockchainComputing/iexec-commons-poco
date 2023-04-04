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

package com.iexec.commons.poco.contract;

import com.iexec.commons.poco.contract.generated.IexecHubContract;
import lombok.extern.slf4j.Slf4j;
import java.math.BigInteger;

@Slf4j
public class IexecHubSmartContractValidator {
    public boolean validate(IexecHubContract contract) {
        final BigInteger finalDeadlineRatio;
        final String errorMessage =
                "Something went wrong with IexecHub smart contract configuration. "
                        + "Please check your configuration.";
        try {
            finalDeadlineRatio = contract
                    .final_deadline_ratio()
                    .send();
        } catch (Exception e) {
            log.error(errorMessage, e);
            return false;
        }

        if (finalDeadlineRatio == null) {
            log.error(errorMessage + " Can't retrieve final deadline ratio");
            return false;
        }

        if (finalDeadlineRatio.compareTo(BigInteger.ZERO) <= 0) {
            log.error(errorMessage + " Final deadline ratio should be positive"
                    + " [finalDeadlineRatio: {}]", finalDeadlineRatio);
            return false;
        }

        log.info("IexecHub smart contract is reachable.");
        return true;
    }
}
