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

After, you need to clone `Poco-dev` repository and compile
```
git clone https://github.com/iExecBlockchainComputing/PoCo-dev/
cd PoCo-dev
git checkout x.y.z
npm i
./node_modules/.bin/truffle compile
```

```
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/App.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/AppRegistry.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/Dataset.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/DatasetRegistry -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/DatasetRegistry.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/Ownable.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/Workerpool.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/WorkerpoolRegistry -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/WorkerpoolRegistry.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated


web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/IexecInterfaceTokenABILegacy.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated
# Rename IexecInterfaceTokenABILegacy.java to IexecHubContract.java

# Clean IexecLibOrders_v5.OrderOperationEnum references from IexecLibOrders_v5.json (Only in abi node), then 
web3j generate truffle --truffle-json ~/iexecdev/PoCo-dev/build/contracts/IexecLibOrders_v5.json -o ~/iexecdev/iexec-commons-poco/src/main/java/ -p com.iexec.commons.poco.contract.generated

```