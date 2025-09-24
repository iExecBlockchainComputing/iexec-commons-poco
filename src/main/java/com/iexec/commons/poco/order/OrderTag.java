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

import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.Getter;

public enum OrderTag {

    STANDARD(BytesUtils.EMPTY_HEX_STRING_32),
    TEE_SCONE(TeeUtils.TEE_SCONE_ONLY_TAG),
    TEE_GRAMINE(TeeUtils.TEE_GRAMINE_ONLY_TAG),
    TEE_TDX(TeeUtils.TEE_TDX_ONLY_TAG);

    @Getter
    private final String value;

    OrderTag(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
