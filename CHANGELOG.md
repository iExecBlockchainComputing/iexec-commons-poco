# Changelog

All notable changes to this project will be documented in this file.

## [[NEXT]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/vNEXT) 2025

### Quality

- Upgrade source and target compatibility to Java 17. (#114)
- Optimize gas limits per PoCo function. This will enable validators
  to add more finalize and contributeAndFinalize transactions in a block. (#116)

### Breaking API changes

- Remove assets creation methods from `IexecHubAbstractService`. (#115)
- Remove AppRegistry, DatasetRegistry and WorkerpoolRegistry generated classes. (#116)

### Dependency Upgrades

- Upgrade to Spring Boot 3.0.13. (#114)

## [[4.2.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v4.2.0) 2024-12-18

### New Features

- Add `getUserGasPrice` method to `Web3jAbstractService`. (#104)
- Make **TEE tasks** with callback eligible to `contributeAndFinalize` flow. (#109)
- Add accessors to read on-chain deployed PoCo Smart Contracts configurations:
  `callbackgas`, `contribution_deadline_ratio` and `final_deadline_ratio`. (#111)

### Bug Fixes

- Prefer methods with ECKEyPair parameter when signing prefixed messages. (#107)

### Quality

- Use `poco-chain` with `poco v5.5.0` and `voucher v1.0.0` in tests. (#106)
- Manage deal parameters in a single field and add assets owner and assets price fields in `TaskDescription`. (#108)

### Dependency Upgrades

- Upgrade to Gradle 8.10.2. (#105)
- Upgrade to `testcontainers` 1.20.4. (#110)

## [[4.1.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v4.1.0) 2024-06-17

### New Features

- Add `AbstractAssetDeploymentService` and move `getNonce` method. (#92)
- Estimate gas and submit a transaction in a single method. (#97)
- Add method to fetch on-chain deal without app or dataset details. (#98)
- Add encodings to call PoCo assets address prediction functions. (#101)

### Quality

- Configure Gradle JVM Test Suite Plugin. (#89)
- Remove unused `predictApp`, `predictDataset` and `predictWorkerpool` methods. (#93)
- Remove empty `ChainStatus` interface. (#94)
- Remove `com.iexec.commons.poco.notification` package. (#95)
- Restrict several methods visibility in `IexecHubAbstractService`. (#96)
- Avoid exceptions during `IexecHubAbstractService` and `Web3jAbstractService` objects creation. (#99)
- Improve `SignatureUtils`: remove dead code and remove cleanly unused parameter in `hashAndSign`. (#100)

### Dependency Upgrades

- Upgrade to Gradle 8.7. (#90)
- Upgrade to Spring Boot 2.7.18. (#91)

## [[4.0.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v4.0.0) 2024-04-12

### New features

- Add `SignerService` class. (#72)
- Add encoders to allow sending transactions with `SignerService`. (#73 #74 #75)
- Add `getAssetAddressFromReceipt` method to `AssetDataEncoder`. (#78)
- Use `eth_call` Ethereum JSON-RPC API to predict assets on-chain address. (#79)
- Add `PoCoDataEncoder` with `initialize`, `contribute`, `reveal`, `finalize` and `contributeAndFinalize` support. (#80 #81)
- Add `eth_estimateGas` Ethereum JSON-RPC API support. (#82)
- Add transaction data encoder to support `isRegistered` method call. (#83)
- Add decoder to display log topics with human readable names. (#84)

### Bug Fixes

- Log a message if a transaction could not be verified on-chain, always return its hash. (#85)

### Quality

- Remove unused `IexecLibOrders_v5` generated class. (#68)
- Use `@SneakyThrows` lombok annotation in `EIP-712` related tests. (#69)
- Migrate `EthAddress` utility class from `iexec-common`. (#71)
- Replace `OrderSigner` with `SignerService` in `MatchOrdersTests`. (#76)
- Add methods to `IexecHubTestService` and add `OrdersService` for tests. (#77)

## [[3.2.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.2.0) 2023-12-19

### New Features

- Add `contributionDeadline` and `finalDeadline` fields to `TaskDescription`. (#65)

### Bug Fixes

- Remove unsupported `post-compute` related fields from `DealParams` and `TaskDescription`. (#58)
- Dataset names can be empty in Poco deals. (#59)
- Catch all exceptions when reading the latest block number on the blockchain network. (#60)
- Remove methods from `IexecHubAbstractService`, they were unused or moved to `iexec-core`. (#63)
- Write PoCo orders classes `toString` implementations with compliant fields ordering. (#64)

### Dependency Upgrades

- Upgrade to Spring Boot 2.7.17. (#62)
- Upgrade to `jenkins-library` 2.7.4. (#61)
- Upgrade to `testcontainers` 1.19.3. (#62)

## [[3.1.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.1.0) 2023-09-25

### New Features

- Add `isEligibleToContributeAndFinalize` method to `TaskDescription`. (#53)
- Use `RawTransactionManager` instance to create App, Dataset and Workerpool on-chain in one block. (#54)

### Quality

- Do not run tests in `itest` task to avoid executing them twice. (#47)
- Upgrade to Gradle 8.2.1 with up-to-date plugins. (#49)
- Fetch contribution deadline ratio during `@PostConstruct` execution in `IexecHubAbstractService`. (#51)
- Remove dead code in `IexecHubAbstractService` and `Web3jAbstractService`. (#54 #55)

### Dependency Upgrades

- Upgrade to Spring Boot 2.7.14. (#48)
- Upgrade to `testcontainers` 1.19.0. (#50)
- Upgrade to `jenkins-library` 2.7.3. (#52)

## [[3.0.5]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.0.5) 2023-06-26

### Bug Fixes

- Fix regression on dataset URI decryption in `TaskDescription`. The regression concerned bad interpretation of IPFS MultiAddress URIs. (#45)

## [[3.0.4]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.0.4) 2023-06-23

### Bug Fixes

- Fix regression on `DealParams` deserialization. (#43)

## [[3.0.3]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.0.3) 2023-06-22

### New Features

- Add IPFS gateways list and replace `convertToURI` with `isMultiAddress` in `MultiAddressHelper`. (#40)

### Quality

- Improve `Web3jAbstractService` as well as its coverage. (#41)

## [[3.0.2]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.0.2) 2023-06-07

### Bug Fixes

- Do not check connection on blockchain node in `Web3jAbstractService` constructor. (#37)
- Properly handle `InterruptedException` instances caught in `WaitUtils`. (#37)

## [[3.0.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v3.0.0) 2023-06-05

### New Features

- Call `IexecHubContract#viewDeal` instead of legacy ABI in `IexecHubAbstractService#getChainDeal`. (#24)
- Representations of on-chain and off-chain objects are now immutable. (#24 #25 #26 #30 #34)

### Bug Fixes

- Do not cast `retryDelay` to `int` in `IexecHubAbstractService` and `Retryer`. (#32)
- Move `blockTime` from `IexecHubAbstractService` to `Web3jAbstractService`. (#33)
- Set some logs to `debug` level in `EIP712Entity`. (#33)

### Quality

- Move methods to get event blocks to `iexec-core`. (#28)
- Add `IexecHubTestService` and `Web3jTestService` classes for tests. (#29)
- Load `IexecHubContract` instance only once in `IexecHubAbstractService`. (#31)

## [[2.0.1]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v2.0.1) 2023-05-22

### New Features

- Add purge cached task descriptions ability. (#20)

### Bug Fixes

- Pull `poco-chain` image before tests. (#18)
- Keep a security factor of 10 for callback gas consumption during `finalize` and `contributeAndFinalize`. (#22)

## [[2.0.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v2.0.0) 2023-05-11

### New Features

- Upgrade `web3j` dependency from 4.8.9 to 4.9.7. (#8)
- Regenerate all wrappers with new web3j cli version. (#8)
- Update the readme file with the generation instructions. (#8)
- Add `PLEASE_CONTRIBUTE_AND_FINALIZE` to `TaskNotificationType`. (#9)
- Add `com.iexec.commmons.poco.order` package. (#10)
- Add `com.iexec.commons.poco.eip712` package. (#11)
- Add `MatchOrdersTests` on nethermind `poco-chain`. (#12)

### Bug Fixes

- Set `protected` visibility on abstract classes constructors. (#13)

## [[1.0.2]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v1.0.2) 2023-04-11

### Bug Fixes

- Remove unused `guava` dependency. (#5)

## [[1.0.0]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/v1.0.0) 2023-04-06

### New Features

- Init Gradle project. (#1 #2)
- Migrate from `iexec-common` library to this `iexec-commons-poco` library packages related to blockchain and iExec PoCo protocol. (#3)
