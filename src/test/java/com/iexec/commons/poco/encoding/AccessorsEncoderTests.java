/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;

import static com.iexec.commons.poco.encoding.AccessorsEncoder.*;
import static org.assertj.core.api.Assertions.assertThat;

class AccessorsEncoderTests {
    private String getSelector(final String methodSignature) {
        return Hash.sha3String(methodSignature).substring(0, 10);
    }

    @Test
    void checkSelectorValues() {
        assertThat(getSelector("callbackgas()")).isEqualTo(CALLBACKGAS_SELECTOR);
        assertThat(getSelector("contribution_deadline_ratio()")).isEqualTo(CONTRIBUTION_DEADLINE_RATIO_SELECTOR);
        assertThat(getSelector("final_deadline_ratio()")).isEqualTo(FINAL_DEADLINE_RATIO_SELECTOR);
        assertThat(getSelector("owner()")).isEqualTo(OWNER_SELECTOR);
        assertThat(getSelector("viewAccount(address)")).isEqualTo(VIEW_ACCOUNT_SELECTOR);
        assertThat(getSelector("viewCategory(uint256)")).isEqualTo(VIEW_CATEGORY_SELECTOR);
        assertThat(getSelector("viewConsumed(bytes32)")).isEqualTo(VIEW_CONSUMED_SELECTOR);
        assertThat(getSelector("viewContribution(bytes32,address)")).isEqualTo(VIEW_CONTRIBUTION_SELECTOR);
        assertThat(getSelector("viewScore(address)")).isEqualTo(VIEW_SCORE_SELECTOR);
        assertThat(getSelector("viewApp(address)")).isEqualTo(VIEW_APP_SELECTOR);
        assertThat(getSelector("viewDataset(address)")).isEqualTo(VIEW_DATASET_SELECTOR);
    }
}
