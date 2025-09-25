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

package com.iexec.commons.poco.encoding;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class containing Ethereum function selectors to read PoCo Smart Contracts configurations.
 * <p>
 * Current accessors allow to read callback gas, contribution deadline ratio and final deadline ratio.
 *
 * @see <a href="https://github.com/iExecBlockchainComputing/PoCo/blob/main/contracts/modules/interfaces/IexecAccessors.sol">PoCo accessors</a>
 * @see <a href="https://docs.soliditylang.org/en/latest/abi-spec.html#function-selector">Ethereum Contract ABI Specification</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccessorsEncoder {

    /**
     * keccak256(callbackgas())
     */
    public static final String CALLBACKGAS_SELECTOR = "0xe63ec07d";

    /**
     * keccak256(contribution_deadline_ratio())
     */
    public static final String CONTRIBUTION_DEADLINE_RATIO_SELECTOR = "0x74ed5244";

    /**
     * keccak256(final_deadline_ratio())
     */
    public static final String FINAL_DEADLINE_RATIO_SELECTOR = "0xdb8aaa26";

    /**
     * keccak256(owner())
     */
    public static final String OWNER_SELECTOR = "0x8da5cb5b";

    public static final String VIEW_CONSUMED_SELECTOR = "0x4b2bec8c";

    // app
    public static final String M_APPCHECKSUM_SELECTOR = "0x84aaf12e";
    public static final String M_APPMRENCLAVE_SELECTOR = "0xe30d26a8";
    public static final String M_APPMULTIADDR_SELECTOR = "0x39e75d45";
    public static final String M_APPTYPE_SELECTOR = "0xf8c2ceb3";

    // dataset
    public static final String M_DATASETCHECKSUM_SELECTOR = "0x1ba99d7e";
    public static final String M_DATASETMULTIADDR_SELECTOR = "0xa61ca6c5";

}
