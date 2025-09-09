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

package com.iexec.commons.poco.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.iexec.commons.poco.chain.ChainDeal;
import com.iexec.commons.poco.chain.ChainTask;
import com.iexec.commons.poco.chain.DealParams;
import com.iexec.commons.poco.dapp.DappType;
import com.iexec.commons.poco.tee.TeeEnclaveConfiguration;
import com.iexec.commons.poco.tee.TeeFramework;
import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.BytesUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskDescription {

    // computed data, not available on-chain
    String chainTaskId;

    // assets
    DappType appType;
    String appUri;
    TeeEnclaveConfiguration appEnclaveConfiguration;
    String datasetUri;
    String datasetChecksum;

    // deal
    String chainDealId;
    String appAddress;
    String appOwner;
    BigInteger appPrice;
    String datasetAddress;
    String datasetOwner;
    BigInteger datasetPrice;
    String workerpoolAddress;
    String workerpoolOwner;
    BigInteger workerpoolPrice;
    BigInteger trust;
    BigInteger category;
    boolean isTeeTask;
    TeeFramework teeFramework;
    String requester;
    String beneficiary;
    String callback;
    @Builder.Default
    DealParams dealParams = DealParams.builder().build();
    long startTime;
    int botSize;
    int botFirstIndex;

    // task
    int botIndex;
    long maxExecutionTime; // timeref ?
    long contributionDeadline;
    long finalDeadline;

    /**
     * Check if this task includes a dataset or not. The task is considered
     * as including a dataset only if all fields of the dataset are non-empty,
     * non-null values. The stack should ignore datasets with missing
     * information since they, inevitably, break the workflow. In the case
     * where those datasets are ignored, the worker will contribute an
     * application error caused by the missing dataset file.
     *
     * @return true if all dataset fields are all non-null,
     * non-empty values, false otherwise.
     */
    public boolean containsDataset() {
        return !StringUtils.isBlank(datasetAddress) &&
                !datasetAddress.equals(BytesUtils.EMPTY_ADDRESS) &&
                !StringUtils.isBlank(datasetUri) &&
                !StringUtils.isBlank(datasetChecksum);
    }

    /**
     * Check if a callback is requested for this task.
     *
     * @return true if a callback address is found in the deal, false otherwise.
     */
    public boolean containsCallback() {
        return !StringUtils.isEmpty(callback) && !callback.equals(BytesUtils.EMPTY_ADDRESS);
    }

    /**
     * Check if this task includes some input files.
     *
     * @return true if at least one input file is present, false otherwise
     */
    public boolean containsInputFiles() {
        return dealParams != null && dealParams.getIexecInputFiles() != null && !dealParams.getIexecInputFiles().isEmpty();
    }

    public String getAppCommand() {
        return dealParams == null || StringUtils.isBlank(dealParams.getIexecArgs()) ? appEnclaveConfiguration.getEntrypoint() :
                appEnclaveConfiguration.getEntrypoint() + " " + dealParams.getIexecArgs();
    }

    /**
     * Returns whether the request is for a bulk operation or not.
     *
     * @return {@literal true} for a bulk operation, {@literal false} otherwise
     */
    public boolean isBulkRequest() {
        return dealParams != null && !StringUtils.isBlank(dealParams.getBulkCid());
    }

    /**
     * A task is eligible to the Contribute And Finalize flow
     * if it matches the following conditions:
     * <ul>
     *     <li>It is a TEE task
     *     <li>Its trust is 1
     *     <li>It does not contain a callback - bug in the PoCo, should be fixed
     * </ul>
     *
     * @return {@literal true} if eligible, {@literal false} otherwise.
     */
    public boolean isEligibleToContributeAndFinalize() {
        return isTeeTask && BigInteger.ONE.equals(trust);
    }

    /**
     * Returns whether pre-compute stage must be executed to retrieve data
     *
     * @return {@literal true} if pre-compute has to be executed, {@literal false}
     */
    public boolean requiresPreCompute() {
        return containsDataset() || containsInputFiles() || isBulkRequest();
    }

    /**
     * Create a {@link TaskDescription} from the provided chain deal. This method
     * if preferred to constructors or the builder method.
     *
     * @param chainDeal On-chain deal from PoCo smart contracts
     * @param chainTask On-chain task from PoCo smart contracts
     * @return the created taskDescription
     */
    public static TaskDescription toTaskDescription(final ChainDeal chainDeal, final ChainTask chainTask) {
        if (chainDeal == null || chainTask == null) {
            return null;
        }
        String datasetUri = "";
        String datasetChecksum = "";
        if (chainDeal.containsDataset()) {
            datasetUri = chainDeal.getChainDataset().getMultiaddr();
            datasetChecksum = chainDeal.getChainDataset().getChecksum();
        }
        final String tag = chainDeal.getTag();
        return TaskDescription.builder()
                .chainTaskId(chainTask.getChainTaskId())
                // assets
                .appType(DappType.DOCKER)
                .appUri(chainDeal.getChainApp().getMultiaddr())
                .appEnclaveConfiguration(chainDeal.getChainApp().getEnclaveConfiguration())
                .datasetUri(datasetUri)
                .datasetChecksum(datasetChecksum)
                // deal
                .chainDealId(chainDeal.getChainDealId())
                .appAddress(chainDeal.getDappPointer())
                .appOwner(chainDeal.getDappOwner())
                .appPrice(chainDeal.getDappPrice())
                .datasetAddress(chainDeal.getDataPointer())
                .datasetOwner(chainDeal.getDataOwner())
                .datasetPrice(chainDeal.getDataPrice())
                .workerpoolAddress(chainDeal.getPoolPointer())
                .workerpoolOwner(chainDeal.getPoolOwner())
                .workerpoolPrice(chainDeal.getPoolPrice())
                .trust(chainDeal.getTrust())
                .category(chainDeal.getCategory())
                .isTeeTask(TeeUtils.isTeeTag(tag))
                .teeFramework(TeeUtils.getTeeFramework(tag))
                .requester(chainDeal.getRequester())
                .beneficiary(chainDeal.getBeneficiary())
                .callback(chainDeal.getCallback())
                .dealParams(chainDeal.getParams())
                .startTime(chainDeal.getStartTime().longValue())
                .botSize(chainDeal.getBotSize().intValue())
                .botFirstIndex(chainDeal.getBotFirst().intValue())
                // task
                .botIndex(chainTask.getIdx())
                .maxExecutionTime(chainDeal.getChainCategory().getMaxExecutionTime()) // https://github.com/iExecBlockchainComputing/PoCo/blob/v5/contracts/modules/delegates/IexecPoco2Delegate.sol#L111
                .contributionDeadline(chainTask.getContributionDeadline())
                .finalDeadline(chainTask.getFinalDeadline())
                .build();
    }
}
