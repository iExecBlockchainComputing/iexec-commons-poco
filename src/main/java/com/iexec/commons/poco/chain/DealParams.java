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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * The DealParams class represents runtime configurations provided for tasks computation.
 * DealParams allow a requester to provide <b>args</b>, <b>input files</b>, <b>secrets</b>,
 * <b>result storage configuration</b>... Those configurations are written on the blockchain
 * when a deal is submitted.
 * <p>
 * The data written on the blockchain can be:
 * <ul>
 * <li> A valid JSON string representing a map with key -&gt; value pairs.
 *      The keys are mapped to DealParams fields: iexec_args, iexec_input_files, iexec_secrets, iexec_developer_logger,
 *      iexec_result_encryption, iexec_result_storage_provider, iexec_result_storage_proxy.
 * <li> A simple string that will be used for <b>iexec_args</b>
 * </ul>
 * <p>
 * The <b>iexec_secrets</b> key -&gt; value map declares which secrets of the requester
 * can be accessed by a targeted TEE application.
 * <ul>
 * <li>A value should hold the secret name of a secret belonging to the requester.
 *     This secret must be previously pushed on the SMS.
 * <li>A key associated to a value indicates how the TEE application can access
 *     that value.
 * </ul>
 * <p>
 * Example:
 * <ul>
 * <li>A requester has a secret value "2000-01-01" pushed on the SMS. The name of
 *     this secret is "my-birthday".
 * <li>A requester has a secret value "+33123456789" pushed on the SMS. The name
 *     of this secret is "my-phone-number".
 * </ul>
 * <p>
 * The application developer creates a TEE application which allows requesters
 * to :
 * <ol>
 * <li>Share their birthday by affecting their secret to position "2"
 * <li>Share their phone number by affecting their secret to position "5"
 * </ol>
 * <p>
 * In order to share birthday and phone number to the TEE application, the
 * requester should declare:
 * <pre>"iexec_secrets" : { "2": "my-birthday" , "5": "my-phone-number" }</pre>
 * <p>
 * By doing this, the TEE application code will be able to read following
 * environment variables:
 * <pre>
 * IEXEC_REQUESTER_SECRET_2=2000-01-01
 * IEXEC_REQUESTER_SECRET_5=+33123456789
 * </pre>
 */
@Slf4j
@Value
@Builder
@JsonDeserialize(builder = DealParams.DealParamsBuilder.class)
public class DealParams {

    public static final String IPFS_RESULT_STORAGE_PROVIDER = "ipfs";
    public static final String DROPBOX_RESULT_STORAGE_PROVIDER = "dropbox";

    // Note to dev: the naming of the variables in the json file is important since it will be stored on-chain
    @JsonProperty("iexec_args")
    String iexecArgs;

    @Builder.Default
    @JsonProperty("iexec_input_files")
    List<String> iexecInputFiles = List.of();

    @JsonProperty("iexec_result_encryption")
    boolean iexecResultEncryption;

    @JsonProperty("iexec_result_storage_provider")
    String iexecResultStorageProvider;

    @JsonProperty("iexec_result_storage_proxy")
    String iexecResultStorageProxy;

    // Keys are app secrets indices, values are requester secrets indices
    @Builder.Default
    @JsonProperty("iexec_secrets")
    Map<String, String> iexecSecrets = Map.of();

    @Builder.Default
    @JsonProperty("bulk_cid")
    String bulkCid = "";

    @JsonPOJOBuilder(withPrefix = "")
    public static class DealParamsBuilder {
    }

    /**
     * Creates an instance from a JSON string representation.
     *
     * @param paramString data to create a DealParams instance.
     *                    The value will be parsed if it is a valid JSON string,
     *                    it will be used as <b>iexecArgs</b> value otherwise.
     *                    In the second case, every other parameters will have default values.
     * @return the created instance
     */
    public static DealParams createFromString(final String paramString) {
        try {
            return new ObjectMapper().readValue(paramString, DealParams.class);
        } catch (IOException e) {
            log.warn("Could not deserialize arguments to DealParams [string:{}]", paramString, e);
            //the requester want to execute one task with the whole string
            return DealParams.builder()
                    .iexecArgs(paramString)
                    .iexecInputFiles(List.of())
                    .iexecSecrets(Map.of())
                    .build();
        }
    }

    /**
     * Represents the instance as a JSON string that will be written on chain.
     *
     * @return the JSON string representing the instance
     */
    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        List.of(NON_EMPTY, NON_DEFAULT).forEach(mapper::setSerializationInclusion);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Deal parameters serialization failed.", e);
            return "";
        }
    }

}
