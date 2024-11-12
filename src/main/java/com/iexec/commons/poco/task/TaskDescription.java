/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.utils.MultiAddressHelper;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskDescription {

    // computed data, not available on-chain
    String chainTaskId;

    // assets
    String appOwner;
    BigInteger appPrice;
    String datasetOwner;
    BigInteger datasetPrice;
    String workerpoolOwner;
    BigInteger workerpoolPrice;

    String requester;
    String beneficiary;
    String callback;
    DappType appType;
    String appUri;
    String appAddress;
    TeeEnclaveConfiguration appEnclaveConfiguration;
    /**
     * @deprecated Use dealParams instead
     */
    @Deprecated(forRemoval = true)
    String cmd;
    boolean isTeeTask;
    TeeFramework teeFramework;
    int botSize;
    int botFirstIndex;
    String datasetAddress;
    String datasetUri;
    String datasetName;
    String datasetChecksum;
    /**
     * @deprecated Use dealParams instead
     */
    @Deprecated(forRemoval = true)
    List<String> inputFiles;
    /**
     * @deprecated Use dealParams instead
     */
    @Deprecated(forRemoval = true)
    boolean isResultEncryption;
    /**
     * @deprecated Use dealParams instead
     */
    @Deprecated(forRemoval = true)
    String resultStorageProvider;
    /**
     * @deprecated Use dealParams instead
     */
    @Deprecated(forRemoval = true)
    String resultStorageProxy;
    String smsUrl;
    /**
     * @deprecated Use dealParams instead
     */
    @Deprecated(forRemoval = true)
    Map<String, String> secrets;
    @Builder.Default
    DealParams dealParams = DealParams.builder().build();
    BigInteger trust;
    // from task
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
        return !StringUtils.isEmpty(datasetAddress) &&
                !datasetAddress.equals(BytesUtils.EMPTY_ADDRESS) &&
                !StringUtils.isEmpty(datasetUri) &&
                !StringUtils.isEmpty(datasetChecksum);
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
        return (dealParams != null && dealParams.getIexecInputFiles() != null && !dealParams.getIexecInputFiles().isEmpty())
                || (inputFiles != null && !inputFiles.isEmpty());
    }

    public String getAppCommand() {
        return StringUtils.isEmpty(dealParams.getIexecArgs()) ? appEnclaveConfiguration.getEntrypoint() :
                appEnclaveConfiguration.getEntrypoint() + " " + dealParams.getIexecArgs();
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
        return isTeeTask
                && BigInteger.ONE.equals(trust)
                && !containsCallback();
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
        String datasetAddress = "";
        String datasetUri = "";
        String datasetName = "";
        String datasetChecksum = "";
        if (chainDeal.containsDataset()) {
            datasetAddress = chainDeal.getChainDataset().getChainDatasetId();
            datasetUri = MultiAddressHelper.convertToURI(chainDeal.getChainDataset().getUri());
            datasetName = chainDeal.getChainDataset().getName();
            datasetChecksum = chainDeal.getChainDataset().getChecksum();
        }
        final String tag = chainDeal.getTag();
        return TaskDescription.builder()
                .chainTaskId(chainTask.getChainTaskId())
                .appOwner(chainDeal.getDappOwner())
                .appPrice(chainDeal.getDappPrice())
                .datasetOwner(chainDeal.getDataOwner())
                .datasetPrice(chainDeal.getDataPrice())
                .workerpoolOwner(chainDeal.getPoolOwner())
                .workerpoolPrice(chainDeal.getPoolPrice())
                .requester(chainDeal
                        .getRequester())
                .beneficiary(chainDeal
                        .getBeneficiary())
                .callback(chainDeal
                        .getCallback())
                .appType(DappType.DOCKER)
                .appUri(BytesUtils.hexStringToAscii(chainDeal.getChainApp()
                        .getUri()))
                .appAddress(chainDeal.getChainApp().getChainAppId())
                .appEnclaveConfiguration(chainDeal.getChainApp()
                        .getEnclaveConfiguration())
                .cmd(chainDeal.getParams()
                        .getIexecArgs())
                .inputFiles(chainDeal.getParams()
                        .getIexecInputFiles())
                .isTeeTask(TeeUtils
                        .isTeeTag(tag))
                .teeFramework(TeeUtils
                        .getTeeFramework(tag))
                .isResultEncryption(chainDeal.getParams()
                        .isIexecResultEncryption())
                .resultStorageProvider(chainDeal.getParams()
                        .getIexecResultStorageProvider())
                .resultStorageProxy(chainDeal.getParams()
                        .getIexecResultStorageProxy())
                .secrets(chainDeal.getParams()
                        .getIexecSecrets())
                .dealParams(chainDeal.getParams())
                .datasetAddress(datasetAddress)
                .datasetUri(datasetUri)
                .datasetName(datasetName)
                .datasetChecksum(datasetChecksum)
                .botSize(chainDeal.getBotSize().intValue())
                .botFirstIndex(chainDeal.getBotFirst().intValue())
                .trust(chainDeal.getTrust())
                // from task
                .botIndex(chainTask.getIdx())
                .maxExecutionTime(chainDeal.getChainCategory().getMaxExecutionTime()) // https://github.com/iExecBlockchainComputing/PoCo/blob/v5/contracts/modules/delegates/IexecPoco2Delegate.sol#L111
                .contributionDeadline(chainTask.getContributionDeadline())
                .finalDeadline(chainTask.getFinalDeadline())
                .build();
    }
}
