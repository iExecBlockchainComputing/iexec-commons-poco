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

package com.iexec.commons.poco.utils;

import io.ipfs.multiaddr.MultiAddress;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class MultiAddressHelper {

    public static final List<String> IPFS_GATEWAYS = List.of(
            "https://ipfs-gateway.v8-bellecour.iex.ec",
            "https://gateway.ipfs.io",
            "https://gateway.pinata.cloud"
    );

    private MultiAddressHelper() {
        throw new UnsupportedOperationException();
    }

    public static boolean isMultiAddress(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        try {
            new MultiAddress(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
