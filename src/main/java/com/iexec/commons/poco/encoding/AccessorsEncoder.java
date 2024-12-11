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

}
