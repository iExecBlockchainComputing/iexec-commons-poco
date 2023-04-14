# iexec-commons-poco
PoCo Java wrappers for the iExec platform


## Steps to generate Java Wrappers for Smart Contracts

First you need to install last version of Web3j CLI, the simplest way to install the Web3j CLI is via the following script:

```
curl -L get.web3j.io | sh && source ~/.web3j/source.sh
```
Verify the installation was successful 
```
web3j -v 
Version: 1.4.2
```

After, you need to clone `Poco-dev` repository
```
git clone https://github.com/iExecBlockchainComputing/PoCo-dev/
cd PoCo-dev
git checkout x.y.z
```

Edit script `generateContractWrappers` and check lines *15* and *18*. If necessary, adjust the directories to match your local work tree.
``` shell
#Put here the PoCo-dev directory that contains smart contracts (JSON files)
POCO_DEV_CONTRACTS_DIRECTORY=${HOME}/iexecdev/PoCo-dev/build/contracts/

#Put here the src/main/java/ directory of commons-poco project
COMMONS_POCO_WRAPPER_DIRECTORY=${HOME}/iexecdev/iexec-commons-poco/src/main/java/
```

Clean `IexecLibOrders_v5.OrderOperationEnum` references from `IexecLibOrders_v5.json` with your favorite editor(Only in abi node).

You can now run the script
``` shell
./generateContractWrappers App AppRegistry AppRegistry Dataset DatasetRegistry Ownable Workerpool WorkerpoolRegistry IexecInterfaceTokenABILegacy IexecLibOrders_v5
```

After this execution and if no error has occurred, you must rename `IexecInterfaceTokenABILegacy.java` to `IexecHubContract.java`
