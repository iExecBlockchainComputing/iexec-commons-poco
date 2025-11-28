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

package com.iexec.commons.poco.chain;

import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

@Value
@Builder
public class ChainDeal {

    String chainDealId;
    /**
     * @deprecated not consistent with Smart Contracts
     */
    @Deprecated(forRemoval = true)
    ChainApp chainApp;
    /**
     * @deprecated not consistent with Smart Contracts
     */
    @Deprecated(forRemoval = true)
    ChainDataset chainDataset;
    /**
     * @deprecated not consistent with Smart Contracts
     */
    @Deprecated(forRemoval = true)
    ChainCategory chainCategory;
    // deal_pt1
    String dappPointer;
    String dappOwner;
    BigInteger dappPrice;
    String dataPointer;
    String dataOwner;
    BigInteger dataPrice;
    String poolPointer;
    String poolOwner;
    BigInteger poolPrice;

    // deal_pt2
    BigInteger trust;
    BigInteger category;
    String tag;
    String requester;
    String beneficiary;
    String callback;
    DealParams params;

    // config
    BigInteger startTime;
    BigInteger botFirst;
    BigInteger botSize;
    BigInteger workerStake;
    BigInteger schedulerRewardRatio;

    public boolean containsDataset() {
        return dataPointer != null && !BytesUtils.EMPTY_ADDRESS.equals(dataPointer);
    }

    public static ChainDeal parts2ChainDeal(String chainDealId, IexecHubContract.Deal deal, ChainCategory category) {
        if (deal == null || category == null) {
            return ChainDeal.builder().build();
        }
        return create(chainDealId, deal, category, null, null);
    }

    /**
     * @deprecated app, category and dataset will no more be embedded in deal to stick to on-chain models
     */
    @Deprecated(forRemoval = true)
    public static ChainDeal parts2ChainDeal(String chainDealId, IexecHubContract.Deal deal, ChainApp app, ChainCategory category, ChainDataset dataset) {
        if (deal == null || app == null || category == null) {
            return ChainDeal.builder().build();
        }
        return create(chainDealId, deal, category, app, dataset);
    }

    private static ChainDeal create(String chainDealId, IexecHubContract.Deal deal, ChainCategory category, ChainApp app, ChainDataset dataset) {
        return ChainDeal.builder()
                .chainDealId(chainDealId)
                .chainApp(app)
                .chainDataset(dataset)
                .chainCategory(category)
                .dappPointer(deal.app.pointer)
                .dappOwner(deal.app.owner)
                .dappPrice(deal.app.price)
                .dataPointer(deal.dataset.pointer)
                .dataOwner(deal.dataset.owner)
                .dataPrice(deal.dataset.price)
                .poolPointer(deal.workerpool.pointer)
                .poolOwner(deal.workerpool.owner)
                .poolPrice(deal.workerpool.price)
                .trust(deal.trust)
                .category(deal.category)
                .tag(BytesUtils.bytesToString(deal.tag))
                .requester(deal.requester)
                .beneficiary(deal.beneficiary)
                .callback(deal.callback)
                .params(DealParams.createFromString(deal.params))
                .startTime(deal.startTime)
                .botFirst(deal.botFirst)
                .botSize(deal.botSize)
                .workerStake(deal.workerStake)
                .schedulerRewardRatio(deal.schedulerRewardRatio)
                .build();
    }
}
