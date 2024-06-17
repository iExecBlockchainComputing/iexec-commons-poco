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

package com.iexec.commons.poco.chain;

import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.utils.BytesUtils;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainDealTest {

    public static final IexecHubContract.Deal DEAL = new IexecHubContract.Deal(
            new IexecHubContract.Resource("0x1", "0x2", BigInteger.valueOf(3)),
            new IexecHubContract.Resource("0x4", "0x5", BigInteger.valueOf(6)),
            new IexecHubContract.Resource("0x7", "0x8", BigInteger.valueOf(9)),
            BigInteger.valueOf(1),
            BigInteger.ZERO, //unused in parts
            new byte[32], "0x3", "0x4", "0x5", "params",
            BigInteger.valueOf(2),
            BigInteger.valueOf(3),
            BigInteger.valueOf(4),
            BigInteger.valueOf(5),
            BigInteger.valueOf(6)
    );
    public static final String CHAIN_DEAL_ID = "chainDeal";

    @Test
    void testEmptyConstructor() {
        final ChainDeal deal = ChainDeal.builder().build();
        assertThat(deal).hasAllNullFieldsOrProperties();
    }

    // region parts2ChainDeal without app and dataset
    @Test
    void shouldGetEmptyChainDealSinceNoDeal() {
        final ChainCategory category = ChainCategory.builder().build();
        final ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID, null, category);
        assertEquals(chainDeal, ChainDeal.builder().build());
    }

    @Test
    void shouldGetEmptyChainDealSinceNoCategory() {
        final ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID, DEAL, null);
        assertEquals(chainDeal, ChainDeal.builder().build());
    }

    @Test
    void shouldConvertToChainDeal() {
        final ChainCategory category = ChainCategory.builder().build();
        final ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID, DEAL, category);

        assertEquals(chainDeal,
                ChainDeal.builder()
                        .chainDealId(CHAIN_DEAL_ID)
                        .chainApp(null)
                        .dappOwner(DEAL.app.owner)
                        .dappPrice(DEAL.app.price)
                        .chainDataset(null)
                        .dataOwner(DEAL.dataset.owner)
                        .dataPrice(DEAL.dataset.price)
                        .poolPointer(DEAL.workerpool.pointer)
                        .poolOwner(DEAL.workerpool.owner)
                        .poolPrice(DEAL.workerpool.price)
                        .trust(DEAL.trust)
                        .tag(BytesUtils.bytesToString(DEAL.tag))
                        .requester(DEAL.requester)
                        .beneficiary(DEAL.beneficiary)
                        .callback(DEAL.callback)
                        .params(DealParams.createFromString(DEAL.params))
                        .chainCategory(category)
                        .startTime(DEAL.startTime)
                        .botFirst(DEAL.botFirst)
                        .botSize(DEAL.botSize)
                        .workerStake(DEAL.workerStake)
                        .schedulerRewardRatio(DEAL.schedulerRewardRatio)
                        .build()
        );

    }
    // endregion

    // region parts2ChainDeal with app and dataset
    @Test
    void shouldConvertToChainDealWithDetails() {
        ChainApp app = ChainApp.builder().build();
        ChainCategory category = ChainCategory.builder().build();
        ChainDataset dataset = ChainDataset.builder().build();

        ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID, DEAL, app, category, dataset);

        assertEquals(chainDeal,
                ChainDeal.builder()
                        .chainDealId(CHAIN_DEAL_ID)
                        .chainApp(app)
                        .dappOwner(DEAL.app.owner)
                        .dappPrice(DEAL.app.price)
                        .chainDataset(dataset)
                        .dataOwner(DEAL.dataset.owner)
                        .dataPrice(DEAL.dataset.price)
                        .poolPointer(DEAL.workerpool.pointer)
                        .poolOwner(DEAL.workerpool.owner)
                        .poolPrice(DEAL.workerpool.price)
                        .trust(DEAL.trust)
                        .tag(BytesUtils.bytesToString(DEAL.tag))
                        .requester(DEAL.requester)
                        .beneficiary(DEAL.beneficiary)
                        .callback(DEAL.callback)
                        .params(DealParams.createFromString(DEAL.params))
                        .chainCategory(category)
                        .startTime(DEAL.startTime)
                        .botFirst(DEAL.botFirst)
                        .botSize(DEAL.botSize)
                        .workerStake(DEAL.workerStake)
                        .schedulerRewardRatio(DEAL.schedulerRewardRatio)
                        .build()
        );
    }

    @Test
    void shouldGetEmptyChainDealWithDetailsSinceNoDeal() {
        ChainApp app = ChainApp.builder().build();
        ChainCategory category = ChainCategory.builder().build();
        ChainDataset dataset = ChainDataset.builder().build();

        ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID,
                null,
                app,
                category,
                dataset);

        assertEquals(chainDeal, ChainDeal.builder().build());
    }

    @Test
    void shouldGetEmptyChainDealWithDetailsSinceNoApp() {
        ChainCategory category = ChainCategory.builder().build();
        ChainDataset dataset = ChainDataset.builder().build();

        ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID,
                DEAL,
                null,
                category,
                dataset);

        assertEquals(chainDeal, ChainDeal.builder().build());
    }

    @Test
    void shouldGetEmptyChainDealWithDetailsSinceNoCategory() {
        ChainApp app = ChainApp.builder().build();
        ChainDataset dataset = ChainDataset.builder().build();

        ChainDeal chainDeal = ChainDeal.parts2ChainDeal(CHAIN_DEAL_ID,
                DEAL,
                app,
                null,
                dataset);

        assertEquals(chainDeal, ChainDeal.builder().build());
    }
    // endregion
}
