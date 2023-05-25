# Changelog

All notable changes to this project will be documented in this file.

## [[NEXT]](https://github.com/iExecBlockchainComputing/iexec-commons-poco/releases/tag/vNEXT) 2023

### New Features
- Call `IexecHubContract#viewDeal` instead of legacy ABI in `IexecHubAbstractService#getChainDeal`. (#24)
- Representations of on-chain object are now immutable. (#24 #25 #26)
### Quality
- Move methods to get event blocks to `iexec-core`. (#28)
- Add `IexecHubService` and `Web3jService` classes for tests. (#29)

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
