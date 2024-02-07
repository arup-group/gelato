# Gelato

![DailyCIbadge](https://github.com/arup-group/gelato/actions/workflows/ci.yml/badge.svg)

<figure>
    <img alt="Gelato Clip Art" height="300" src="images/gelato.png" title="Gelato" width="150"/>
    <figcaption><sub>Image Designed by Wannapik</sub></figcaption>
</figure>

<p>

Gelato is a command-line post-processing tool to turn
[MATSim](https://github.com/matsim-org/matsim-libs/tree/master#readme) ABM outputs into Key Performance
Indicator (KPI) metrics. These KPI metrics provide a snapshot summary of a given MATSim
model, and are useful for comparing simulations with each other.


# Installation

If you have Docker installed and would prefer to use Gelato via Docker rather than
locally installing the prerequisite dependencies, then building and running the Java
application from the command line, skip to the
"[Using Gelato via Docker](#using-gelato-via-docker)" section.

## Prerequisites
- JDK >= 17 (start [here](https://www.oracle.com/java/technologies/downloads/) or [here](https://jdk.java.net/))
- [Maven](https://maven.apache.org/)

## Building
To compile everything, run all unit tests, and build a runnable jar file that includes all
the necessary dependencies, clone this repo, and then from the directory you cloned it into:

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

You can run Gelato from the command line, directly from the jar file. The CLI is quite discoverable:

```shell
java -jar target/gelato-1.0-SNAPSHOT-92b26e8.jar --help

Usage: MatsimKpiGenerator [-hV] -mc=<matsimConfigFile>
                          -mo=<matsimOutputDirectory> -o=<outputDir>

  -h, --help        Show this help message and exit.
  -mc=<matsimConfigFile>
                    Full path to your model's MATSim config file
  -mo=<matsimOutputDirectory>
                    Full path to your model's MATSim output directory
  -o=<outputDir>    Full path to the directory you want KPIs to be written to
  -V, --version     Print version information and exit.

```

To generate KPI metrics in a local directory on your machine, assuming:

- MATSim output directory is at `/path/to/my-model/outputs`
- MATSim config file at `/path/to/my-model/outputs/output_config.xml`
- Target output directory at `/path/to/gelato-outputs/my-model/kpi`

```shell
java -jar target/gelato-1.0-SNAPSHOT-92b26e8.jar \
-mc /path/to/my-model/outputs/output_config.xml \
-mo /path/to/my-model/outputs \
-o /path/to/gelato-outputs/my-model/kpi
```

We recommend using MATSim's output config file for the `-mc` parameter wherever
possible.

Gelato will read in MATSim's output files and generate a number of output
files in the directory you specified via `-o`.


# Using Gelato via Docker

## Building the Image
You can build a Gelato Docker image by cloning this repo, and then running the
following command from the directory you cloned it into:

```shell
docker build -t gelato .
```

We have tried to keep the Docker image size reasonably small (around 350MB).

## Running Gelato via the Image
Assuming:

- A MATSim output directory at `/path/to/my-model/outputs`
- A MATSim config file at `/path/to/my-model/outputs/output_config.xml`
- A target KPI directory at `/path/to/gelato-outputs/my-model`

You can run Gelato from the Docker image you just built by mounting
the input and output data directories as volumes in the Docker container
via a command like this:

```shell
docker run \
-v /path/to/my-model/outputs/:/my-model-outputs \
-v /path/to/gelato-outputs/my-model:/gelato-out \
gelato \
-mc /my-model-outputs/output_config.xml \
-mo /my-model-outputs \
-o /gelato-out
```

# The KPIs

## Methodology
Much of the work in creating Gelato involved defining useful KPIs that can be
calculated from data available from a MATSim simulation. The KPIs are grouped
together under various "Themes", strongly influenced by the
[World Business Council for Sustainable Development](https://www.wbcsd.org/)'s
"dimensions" of [sustainable mobility](https://docs.wbcsd.org/2015/03/Mobility_indicators.pdf).

Some KPIs do not fall directly under a specific theme, but are parameters
for generating other KPIs. They are marked as `Parameter` in the table
below. Parameters are included to provide context on the simulation and allow
for sense-checking, whereas KPIs are intended to allow for comparison across
scenarios.

## The KPIs
We will add more KPIs as we go, but for now, these are the KPIs generated
by Gelato:

| Themes                                                                   | KPI Name       | File                     | Definition                                                                                    | Methodology                                                                                                                                                        |
|--------------------------------------------------------------------------|----------------|--------------------------|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| <ul><li>Global Environment</li><li>Mobility System Performance</li></ul> | Congestion     | `kpi-congestion.csv`     | Delays in road traffic and in public transport during peak hours compared to free flow travel | Capture free-flow time at the link level, subtract congested time from this value. Congested time is the difference between link entry and exit time.              |
| <ul><li>Quality of Life</li><li>Mobility System Performance</li></ul>    | PT Wait Time   | `kpi-pt-wait-time.csv`   | Average time waiting for a PT boarding                                                        | Average trip wait times by transport mode.                                                                                                                         |
| Parameter                                                                | Occupancy Rate | `kpi-occupancy-rate.csv` | Average load factor of vehicles of all modes                                                  | Track boarding/alighting events at vehicle level and combine with the vehicle log to calculate distances, then aggregate to average occupancy by mode.             |
| Parameter                                                                | Vehicle KM     | `kpi-vehicle-km.csv`     | Total distance travelled by all moving vehicles                                               | Sum the total distance travelled as recorded in the trip logs.                                                                                                     |
| Parameter                                                                | Speed          | `kpi-speed.csv`          | Network link length divided by travel time                                                    | Calculate average speed for each network link in hourly bins.                                                                                                      |
| Parameter                                                                | Modal Split    | `kpi-modal-split.csv`    | Modal split of dominant (by distance) trip modes                                              | Using trip logs, calculate the number of trips for each mode, as well as the percentage. This metric will not be scaled, but viewed in tandem with the other KPIs. |

## Supporting Data

In addition to KPI files, Gelato also generates a number of "supporting data" files. These files
allow the user to perform their own analyses using aggregations created by Gelato as the basis
for KPI calculations. These files use the naming scheme `supporting-data-<name of file>`.

One such example is the "Link Log" (`supporting-data-linkLog.csv`), which is a data table
recording every vehicle's entry into and exit from each network link it travelled through,
alongside the number of occupants in the vehicle at each of these points. Gelato builds
this table by parsing raw link entry/exit events from MATSim's `output_events.xml.gz` and
then uses it to calculate KPIs such as congestion and occupancy.