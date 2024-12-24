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

package com.iexec.commons.poco.chain;

import com.iexec.commons.poco.utils.WaitUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Async;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import static com.iexec.commons.poco.chain.ChainUtils.weiToEth;
import static com.iexec.commons.poco.contract.generated.AppRegistry.FUNC_CREATEAPP;
import static com.iexec.commons.poco.contract.generated.DatasetRegistry.FUNC_CREATEDATASET;
import static com.iexec.commons.poco.contract.generated.IexecHubContract.*;
import static com.iexec.commons.poco.contract.generated.WorkerpoolRegistry.FUNC_CREATEWORKERPOOL;

@Slf4j
public abstract class Web3jAbstractService {

    static final long GAS_LIMIT_CAP = 1_000_000;

    @Getter
    private final int chainId;
    private final String chainNodeAddress;
    @Getter
    private final Duration blockTime;
    private final float gasPriceMultiplier;
    private final long gasPriceCap;
    private final boolean isSidechain;
    @Getter
    private final Web3j web3j;
    @Getter
    private final ContractGasProvider contractGasProvider;

    /**
     * Apart from initializing usual business entities, it initializes a single
     * and shared web3j instance. This inner web3j instance allows to connect to
     * a remote blockchain node.
     * <p>
     * If reusing a whole web3j instance between calls might be overkilled, it
     * is important to use a single and shared HttpService.
     * The usage of a single HttpService ensures the creation of a single
     * OkHttpClient which ensures a proper connection pool management
     * guaranteeing sockets are properly reused.
     *
     * @param chainId            ID of the blockchain network
     * @param chainNodeAddress   address of the blockchain node
     * @param blockTime          block time as a duration
     * @param gasPriceMultiplier gas price multiplier
     * @param gasPriceCap        gas price cap
     * @param isSidechain        true if iExec native chain, false if iExec token chain
     */
    protected Web3jAbstractService(
            int chainId,
            String chainNodeAddress,
            Duration blockTime,
            float gasPriceMultiplier,
            long gasPriceCap,
            boolean isSidechain) {
        this.chainId = chainId;
        this.chainNodeAddress = chainNodeAddress;
        if (blockTime == null || blockTime.toMillis() <= 0) {
            String message = "Block time value is incorrect, should be a positive integer [blockTime:" + blockTime + "]";
            log.warn(message);
            throw new IllegalArgumentException(message);
        }
        this.blockTime = blockTime;
        this.gasPriceMultiplier = gasPriceMultiplier;
        this.gasPriceCap = gasPriceCap;
        this.isSidechain = isSidechain;
        this.web3j = Web3j.build(new HttpService(chainNodeAddress), this.blockTime.toMillis(), Async.defaultExecutorService());
        this.contractGasProvider = getWritingContractGasProvider();
    }

    @PostConstruct
    public boolean isConnected() {
        try {
            if (web3j.web3ClientVersion().send().getWeb3ClientVersion() != null) {
                log.info("Connected to Ethereum node [address:{}, version:{}]", chainNodeAddress, web3j.web3ClientVersion().send().getWeb3ClientVersion());
                return true;
            }
        } catch (IOException e) {
            log.error("Connection check failed", e);
        }
        return false;
    }

    public static BigInteger getMaxTxCost(long gasPriceCap) {
        return BigInteger.valueOf(GAS_LIMIT_CAP * gasPriceCap);
    }

    // region JSON-RPC
    public EthBlock.Block getLatestBlock() throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
    }

    /**
     * Gets the latest block number from the blockchain network.
     * <p>
     * All exceptions are caught in order to always provide a numerical result.
     *
     * @return the block number, {@literal 0L} otherwise.
     */
    public long getLatestBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber().longValue();
        } catch (Exception e) {
            log.error("ethBlockNumber call failed", e);
        }
        return 0L;
    }

    public EthBlock.Block getBlock(long blockNumber) throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)),
                false).send().getBlock();
    }

    /**
     * @deprecated Use {@link SignerService#getNonce()}
     */
    @Deprecated(forRemoval = true)
    public BigInteger getNonce(String address) {
        try {
            return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                    .send().getTransactionCount();
        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

    public TransactionReceipt getTransactionReceipt(String txHash) {
        try {
            return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    // endregion

    // check if the blockNumber is already available for the scheduler
    // blockNumber is different than 0 only for status the require a check on the blockchain, so the scheduler should
    // already have this block, otherwise it should wait for a maximum of 10 blocks.
    public boolean isBlockAvailable(long blockNumber) {
        // if the block number is already available then simply returns true
        if (blockNumber <= getLatestBlockNumber()) {
            return true;
        }

        // otherwise we wait for a maximum of 10 blocks to see if the block will be available
        try {
            long maxBlockNumber = blockNumber + 10;
            long currentBlockNumber = getLatestBlockNumber();
            while (currentBlockNumber <= maxBlockNumber) {
                if (blockNumber <= currentBlockNumber) {
                    return true;
                } else {
                    log.warn("Chain is NOT synchronized yet [blockNumber:{}, currentBlockNumber:{}]", blockNumber, currentBlockNumber);
                    Thread.sleep(500);
                }
                currentBlockNumber = getLatestBlockNumber();
            }
        } catch (InterruptedException e) {
            log.error("Error when checking the latest block number", e);
            Thread.currentThread().interrupt();
        }

        return false;
    }

    public long getAverageTimePerBlock() {//in ms
        long defaultTime = TransactionManager.DEFAULT_POLLING_FREQUENCY; // 15sec
        int NB_OF_BLOCKS = 10;

        try {
            EthBlock.Block latestBlock = getLatestBlock();

            long latestBlockNumber = latestBlock.getNumber().longValue();

            BigInteger latestBlockTimestamp = latestBlock.getTimestamp();
            BigInteger tenBlocksAgoTimestamp = getBlock(latestBlockNumber - NB_OF_BLOCKS).getTimestamp();

            defaultTime = ((latestBlockTimestamp.longValue() - tenBlocksAgoTimestamp.longValue()) / NB_OF_BLOCKS) * 1000L;
        } catch (IOException e) {
            log.error("Failed to getAverageTimePerBlock", e);
        }
        return defaultTime;
    }

    public boolean hasEnoughGas(String address) {
        // if a sidechain is used, there is no need to check if the wallet has enough gas.
        // if mainnet is used, the check should be done.
        if (isSidechain) {
            return true;
        }

        Optional<BigInteger> optionalBalance = getBalance(address);
        if (optionalBalance.isEmpty()) {
            return false;
        }

        BigInteger weiBalance = optionalBalance.get();
        BigInteger estimateTxNb = weiBalance.divide(getMaxTxCost(gasPriceCap));
        BigDecimal balanceToShow = weiToEth(weiBalance);

        if (estimateTxNb.compareTo(BigInteger.ONE) < 0) {
            log.error("ETH balance is empty, please refill gas now [balance:{}, estimateTxNb:{}]", balanceToShow, estimateTxNb);
            return false;
        } else if (estimateTxNb.compareTo(BigInteger.TEN) < 0) {
            log.warn("ETH balance very low, should refill gas now [balance:{}, estimateTxNb:{}]", balanceToShow, estimateTxNb);
        } else {
            log.debug("ETH balance is fine [balance:{}, estimateTxNb:{}]", balanceToShow, estimateTxNb);
        }

        return true;
    }

    public Optional<BigInteger> getBalance(String address) {
        try {
            BigInteger balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
            log.debug("{} current balance is {}", address, balance);
            return Optional.of(balance);
        } catch (IOException e) {
            log.error("ethGetBalance call failed", e);
            return Optional.empty();
        }
    }

    /**
     * Request current gas price on the network.
     * <p>
     * Note: Some nodes on particular Ethereum networks might reply with
     * versatile gas price values, like 8000000000@t then 0@t+1.
     *
     * @return gas price if greater than zero, else empty
     */
    public Optional<BigInteger> getNetworkGasPrice() {
        try {
            final BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            if (gasPrice != null && gasPrice.signum() > 0) {
                return Optional.of(gasPrice);
            }
        } catch (IOException e) {
            log.error("Failed to get gas price", e);
        }
        return Optional.empty();
    }

    public BigInteger getUserGasPrice(float gasPriceMultiplier, long gasPriceCap) {
        final Optional<BigInteger> networkGasPrice = getNetworkGasPrice();
        if (networkGasPrice.isEmpty()) {
            log.warn("Undefined network gas price (will use default) " +
                    "[userGasPriceCap:{}]", gasPriceCap);
            return BigInteger.valueOf(gasPriceCap);
        }
        final long wishedGasPrice = (long) (networkGasPrice.get().floatValue() * gasPriceMultiplier);

        return BigInteger.valueOf(Math.min(wishedGasPrice, gasPriceCap));
    }

    /**
     * Returns gas price following current user parameters defined in the {@code Web3jAbstractService} instance.
     * <p>
     * The gas price will be Min(gasPriceCap, networkGasPrice * gasPriceMultiplier)
     *
     * @return The gas Price as a {@code BigInteger}
     */
    public BigInteger getUserGasPrice() {
        return getUserGasPrice(gasPriceMultiplier, gasPriceCap);
    }

    private ContractGasProvider getWritingContractGasProvider() {
        return new ContractGasProvider() {

            @Override
            public BigInteger getGasPrice(String s) {
                return getUserGasPrice(gasPriceMultiplier, gasPriceCap);
            }

            @Override
            public BigInteger getGasPrice() {
                return getUserGasPrice(gasPriceMultiplier, gasPriceCap);
            }

            @Override
            public BigInteger getGasLimit(String functionName) {
                return getGasLimitForFunction(functionName);
            }

            @Override
            public BigInteger getGasLimit() {
                return BigInteger.valueOf(GAS_LIMIT_CAP);
            }
        };
    }

    @NotNull
    static BigInteger getGasLimitForFunction(String functionName) {
        long gasLimit = switch (functionName) {
            case FUNC_INITIALIZE -> 300_000;//seen 176340
            case FUNC_CONTRIBUTE -> 500_000;//seen 333541
            case FUNC_REVEAL -> 100_000;//seen 56333
            case FUNC_CONTRIBUTEANDFINALIZE, FUNC_FINALIZE ->
                // Multiply with a factor of 10 for callback gas consumption
                    3_000_000;//seen 175369 (242641 in reopen case)
            case FUNC_REOPEN -> 500_000;//seen 43721
            case FUNC_CREATEAPP -> 900_000;//800000 might not be enough
            case FUNC_CREATEWORKERPOOL -> 700_000;
            case FUNC_CREATEDATASET -> 700_000;//seen 608878
            default -> GAS_LIMIT_CAP;
        };
        return BigInteger.valueOf(gasLimit);
    }

    /*
     * Below method:
     *
     * - checks any function `boolean myMethod(String s1, String s2, ...)`
     * - waits a certain amount of time between checks (waits a certain number of blocks)
     * - stops checking after a certain number of tries
     *
     * */
    //TODO: Add a cache for getAverageTimePerBlock();
    public boolean repeatCheck(int nbBlocksToWaitPerTry, int maxTry, String logTag, Function<String[], Boolean> function, String... functionArgs) {
        if (maxTry < 1) {
            maxTry = 1;
        }

        if (nbBlocksToWaitPerTry < 1) {
            nbBlocksToWaitPerTry = 1;
        }

        long timePerBlock = this.getAverageTimePerBlock();
        long msToWait = nbBlocksToWaitPerTry * timePerBlock;

        int i = 0;
        while (i < maxTry) {
            if (function.apply(functionArgs)) {
                log.info("Verified check [try:{}, function:{}, args:{}, maxTry:{}, msToWait:{}, msPerBlock:{}]",
                        i + 1, logTag, functionArgs, maxTry, msToWait, timePerBlock);
                return true;
            }
            i++;
            WaitUtils.sleepMs(msToWait);
        }

        log.error("Still wrong check [function:{}, args:{}, maxTry:{}, msToWait:{}, msPerBlock:{}]",
                logTag, functionArgs, maxTry, msToWait, timePerBlock);
        return false;
    }
}
