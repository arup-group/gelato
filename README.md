# Gelato (working title)

A command-line post-processing tool to turn MATSim ABM outputs into KPI metrics.

Gelato sits downstream of a [MATSim](https://github.com/matsim-org/matsim-libs/tree/master#readme) agent-based
transport simulation and consumes outputs from the simulation. These outputs are used to generate Key Performance
Indicator (KPI) metrics that measure and document the behaviour of the simulation.

# Installation

## Prerequisites
- JDK >= 17 (start [here](https://www.oracle.com/java/technologies/downloads/))
- [Maven](https://maven.apache.org/)

## Building
To compile everything, run all unit tests, and build a runnable jar file that includes all
the necessary dependencies:

```shell
mvn clean package
```

This will generate a new jar file in the `target` directory that is named according to the Gelato
version number and the latest git commit level - `gelato-1.0-SNAPSHOT-92b26e8.jar` is the runnable
jar in the example below:

```shell
ls -talh target

total 48
-rw-r--r--@  1 mickyfitz  staff   4.0K 13 Dec 15:17 gelato-1.0-SNAPSHOT-92b26e8.jar
drwxr-xr-x@ 12 mickyfitz  staff   384B 13 Dec 15:17 .
-rw-r--r--@  1 mickyfitz  staff   3.6K 13 Dec 15:17 gelato-1.0-SNAPSHOT.jar
drwxr-xr-x@  3 mickyfitz  staff    96B 13 Dec 15:17 maven-archiver
-rw-r--r--@  1 mickyfitz  staff    14K 13 Dec 15:17 jacoco.exec
drwxr-xr-x@  4 mickyfitz  staff   128B 13 Dec 15:17 surefire-reports
drwxr-xr-x@  3 mickyfitz  staff    96B 13 Dec 15:17 test-classes
drwxr-xr-x@  3 mickyfitz  staff    96B 13 Dec 15:17 generated-test-sources
drwxr-xr-x@  3 mickyfitz  staff    96B 13 Dec 15:17 classes
drwxr-xr-x@  3 mickyfitz  staff    96B 13 Dec 15:17 maven-status
drwxr-xr-x@  3 mickyfitz  staff    96B 13 Dec 15:17 generated-sources
drwxr-xr-x@ 15 mickyfitz  staff   480B 13 Dec 15:17 ..
```


# Usage

You can run the tool from the command line, via the jar file:

```shell
java -jar target/gelato-1.0-SNAPSHOT-92b26e8.jar

Making KPI metrics...
```

# The KPIs
