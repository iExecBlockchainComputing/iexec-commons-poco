/*
 * Copyright 2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.commons.poco.itest;

import com.iexec.commons.poco.chain.IexecHubAbstractService;
import com.iexec.commons.poco.chain.Web3jAbstractService;
import org.web3j.crypto.Credentials;

public class IexecHubTestService extends IexecHubAbstractService {
    private static final String IEXEC_HUB_ADDRESS = "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca";
    public IexecHubTestService(Credentials credentials, Web3jAbstractService web3jAbstractService) {
        super(credentials, web3jAbstractService, IEXEC_HUB_ADDRESS);
    }
}
