# NeqSim
NeqSim (Non-Equilibrium Simulator) is a Java library for estimation of fluid behavior and process design for oil and gas production.
The basis for NeqSim is a library of fundamental mathematical models related to phase behavior and physical properties of oil and gas.  NeqSim is easilly extended with new method.

## Releases
[NeqSim releases](https://github.com/equinor/neqsimsource/releases) are available as java packages (jar files) and as source code. NeqSim can be used in a third party application by adding NeqSimSource-x.x-shaded.jar to the classpath.

## Getting Started
See the [NeqSim Java Wiki](https://github.com/equinor/neqsimsource/wiki) for how to use the NeqSim API.
NeqSim can be built using the Maven build system (https://maven.apache.org/). All NeqSim build dependencies are given in the pom.xml file.

### Prerequisites
NeqSim source code can be compiled with JDK8+ and is dependent on a number of third part java libraries. The needed java libraries are listed in the pom.xml file and on the [NeqSim dependencies page](https://github.com/equinor/neqsimsource/network/dependencies).

### Initial setup
NeqSim sorce code is downloaded by cloning the library to your local computer (alternatively fork it to your private reprository). The following commands are dependent on a local installation of [GIT](https://git-scm.com/) and [Maven](https://maven.apache.org/).

```bash
git clone https://github.com/equinor/neqsimsource.git
cd neqsimsource
mvn install
```

## Running the tests
The test files are written in JUnit5 and placed in the [test directory](https://github.com/equinor/neqsimsource/tree/master/src/test). All test have to be passed before merging to the master. Test code shuld be written for all new code added to the project. 

## Deployment
The NeqSim source code is compiled and distributed as a Java library. [NeqSim releases](https://github.com/equinor/neqsimsource/releases) are available for download from the release pages.

## Built With
[Maven](https://maven.apache.org/) - Dependency Management

## Contributing
Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests.

## Versioning
NeqSim use [SemVer](https://semver.org/) for versioning.

## Authors
Even Solbraa, Marlene Louise Lund

## License
NeqSim is distributed under the [Apache-2.0](https://github.com/equinor/neqsimsource/blob/master/LICENSE) licence.

## Acknowledgments
A number of master and PhD students at NTNU have contributed to development of NeqSim. We greatly acknowledge their contributions.

## NeqSim modules
NeqSim is built upon six base modules:
1. Thermodynamic Routines
2. Physical Properties Routines
3. Fluid Mechanic Routines
4. Unit Operations
5. Chemical Reactions Routines
6. Parameter Fitting Routines
7. Process simulation routines


## File System

>neqsim/: main library with all modules 
>
>neqsim/thermo/: Main path for thermodynamic routines
>neqsim/thermo/util/examples/: examples of use of Thermodynamic Models and Routines
>
>neqsim/thermodynamicOperation: Main path for flash routines (TPflash, phase envelopes, etc.)
>neqsim/thermodynamicOperation/util/example/: examples of use of thermodynamic operations (eg. flash calculations etc.)
>
>neqsim/physicalProperties: Main path for Physical Property methods
>neqsim/physicalProperties/util/examples/: Examples of use of physical properties calculations
>
>neqsim/physicalProperties: Main path for Physical Property methods
>neqsim/physicalProperties/util/examples/: Examples of use of physical properties calculations
>
>neqsim/processSimulation: Main path for Process Simulation Calculations
>neqsim/processSimulation/util/examples/: Examples of use of Process Simulation calculations
>
>changelog.txt : History of what changed between each version.
>license.txt: license document
