# Changelog

All notable changes to this project will be documented in this file.

## [[NEXT]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/vNEXT) 2023

### Quality
- Do not run tests in `itest` task to avoid executing them twice. (#47)
- Upgrade to Gradle 8.2.1 with up-to-date plugins. (#49)
- Fetch contribution deadline ratio during `@PostConstruct` execution in `IexecHubAbstractService`. (#51)
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
### Bug Fixes
### Quality
### Dependency Upgrades
