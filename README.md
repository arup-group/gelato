# Gelato

![DailyCIbadge](https://github.com/arup-group/gelato/actions/workflows/ci.yml/badge.svg)

<figure>
    <img alt="Gelato Clip Art" height="300" src="images/gelato.png" title="Gelato" width="150"/>
    <figcaption><sub>Image Designed by Wannapik</sub></figcaption>
</figure>

<p>

Gelato is a [MATSim](https://github.com/matsim-org/matsim-libs/tree/master#readme)-flavoured reference implementation
of the transport Key Performance Indicators (KPI) framework described at
[insert link to KPI framework blog post/other](). Gelato exists to provide a runnable application with which to better
understand and explore the framework, and as such, should be regarded as a by-product of the framework rather
than a work in its own right.

The KPI metrics generated by Gelato provide a snapshot summary of a
given MATSim model, and help compare different MATSim models and scenarios against each other.


# Installation

If you have Docker installed and would prefer to use Gelato via Docker rather than
locally installing the prerequisite dependencies and building and running the Java
application from the command line, skip to the
"[Using Gelato via Docker](#using-gelato-via-docker)" section.

Alternatively, you can grab the latest pre-built, runnable Gelato jar file from the
[releases page](https://github.com/arup-group/gelato/releases), make sure you have the necessary
[prerequisites](#prerequisites) installed and then skip straight to the "[Usage](#usage)" section.

To find out how to build Gelato from source and then run it without using Docker, read on...

## Prerequisites
In order to build and run Gelato locally, your environment must already have available:

#### JDK >= 17
- Start [here](https://www.oracle.com/java/technologies/downloads/) or [here](https://jdk.java.net/)
- If you want to run Gelato from a pre-built jar file rather than build it yourself, you can install a JRE
rather than a full JDK

#### Maven
- Start [here](https://maven.apache.org/)
- Only required if you're building Gelato from source

## Building
To compile everything, run all unit tests and linting checks, and build a runnable jar file that includes all
the necessary dependencies, clone this repo, and then from the directory you cloned it into:

```shell
mvn clean package
```

This will generate a new jar file in the `target` directory that is named according to the Gelato
version number and the latest git commit level - `gelato-0.0.1-alpha-with-dependencies-230f897.jar`
is the runnable jar in the example below:

```shell
ls -talh target
```

```
total 48
-rw-r--r--@  1 mickyfitz  staff    83M 13 Dec 15:17 gelato-0.0.1-alpha-with-dependencies-230f897.jar
drwxr-xr-x@ 12 mickyfitz  staff   384B 13 Dec 15:17 .
-rw-r--r--@  1 mickyfitz  staff    28K 13 Dec 15:17 gelato-0.0.1-alpha.jar
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
java -jar target/gelato-0.0.1-alpha-with-dependencies-230f897.jar --help
```

```
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
java -jar target/gelato-0.0.1-alpha-with-dependencies-230f897.jar \
-mc /path/to/my-model/outputs/output_config.xml \
-mo /path/to/my-model/outputs \
-o /path/to/gelato-outputs/my-model/kpi
```

We recommend using MATSim's output config file for the `-mc` parameter wherever
possible.

Gelato will read in MATSim's output files and generate a number of compressed output
files in the directory you specified via `-o`, giving you something like this:

```shell
-rw-r--r--@  1 mickyfitz  staff   1.5M 19 Feb 12:00 intermediate-congestion.csv.gz
-rw-r--r--@  1 mickyfitz  staff    68K 19 Feb 12:00 intermediate-occupancy-rate.csv.gz
-rw-r--r--@  1 mickyfitz  staff   1.2K 19 Feb 11:59 intermediate-pt-wait-time.csv.gz
-rw-r--r--@  1 mickyfitz  staff    81K 19 Feb 12:00 intermediate-vehicle-km.csv.gz
-rw-r--r--@  1 mickyfitz  staff    97B 19 Feb 12:00 kpi-congestion.csv.gz
-rw-r--r--@  1 mickyfitz  staff   114B 19 Feb 11:59 kpi-modal-split.csv.gz
-rw-r--r--@  1 mickyfitz  staff    24B 19 Feb 12:00 kpi-occupancy-rate.csv.gz
-rw-r--r--@  1 mickyfitz  staff    26B 19 Feb 11:59 kpi-pt-wait-time.csv.gz
-rw-r--r--@  1 mickyfitz  staff   326K 19 Feb 12:00 kpi-speed.csv.gz
-rw-r--r--@  1 mickyfitz  staff    30B 19 Feb 12:00 kpi-vehicle-km.csv.gz
-rw-r--r--@  1 mickyfitz  staff    37K 19 Feb 11:59 supporting-data-legs.csv.gz
-rw-r--r--@  1 mickyfitz  staff    51M 19 Feb 11:59 supporting-data-linkLog.csv.gz
-rw-r--r--@  1 mickyfitz  staff   995K 19 Feb 11:59 supporting-data-networkLinkModes.csv.gz
-rw-r--r--@  1 mickyfitz  staff   3.1M 19 Feb 11:59 supporting-data-networkLinks.csv.gz
-rw-r--r--@  1 mickyfitz  staff   2.6K 19 Feb 11:59 supporting-data-scheduleRoutes.csv.gz
-rw-r--r--@  1 mickyfitz  staff    34K 19 Feb 11:59 supporting-data-scheduleStops.csv.gz
-rw-r--r--@  1 mickyfitz  staff    21K 19 Feb 11:59 supporting-data-trips.csv.gz
-rw-r--r--@  1 mickyfitz  staff    16M 19 Feb 11:59 supporting-data-vehicleOccupancy.csv.gz
-rw-r--r--@  1 mickyfitz  staff    68K 19 Feb 11:59 supporting-data-vehicles.csv.gz
```

For a short explanation of the content and meaning of these files, see [the KPI section](#the-kpis). For a more
comprehensive description, see [insert blog post link here]().

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

- A MATSim output directory on your local machine at `/path/to/my-model/outputs`
- A MATSim config file on your local machine at `/path/to/my-model/outputs/output_config.xml`
- A target KPI output directory on your local machine at `/path/to/gelato-outputs/my-model`
- A desired path to the MATSim files directory inside the Docker container of `/my-model-outputs`
- A desired path to the Gelato KPI output directory inside the Docker container of `/gelato-out`

You can run Gelato from the Docker image you just built by mounting
the input and output data directories as [volumes](https://docs.docker.com/storage/volumes/) in the Docker container
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

You can use Gelato's CLI in essentially the same way you would when running directly from the jar file, for example:

```shell
docker run gelato --version
```
```
0.0.1-alpha
```

```shell
docker run gelato --help
```

```
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

# The KPIs

## Methodology
Much of the work that led to the creation of Gelato involved defining
[a framework of useful KPIs (link to blog post here)]() that can be calculated using data available from a MATSim
simulation. The KPIs are grouped together under "themes", strongly influenced by the
[World Business Council for Sustainable Development](https://www.wbcsd.org/) dimensions of
[sustainable mobility](https://docs.wbcsd.org/2015/03/Mobility_indicators.pdf) and the three pillars of
sustainable development. The themes that we use to describe our KPIs are outlined in column "KPI Theme".

| WBCSD Dimension of Sustainable Mobility | Sustainable Development Pillar | KPI Theme                     |
|-----------------------------------------|--------------------------------|-------------------------------|
| "Global Environment"                    | "Environmental"                | "Environment"                 |
| "Quality of Life"                       | "Social"                       | "Social"                      |
| "Economic Success"                      | "Economic"                     | "Economic"                    |
| "Mobility System Performance"           | --                             | "Mobility System Performance" |

Spanning across all themes, a fourth dimension, "Mobility System Performance" is effectively a separate, unifying layer, whereby improvements in mobility system performance can lead to better outcomes across any or all of the other themes. 

Some KPIs do not fall directly under a specific theme, but are parameters
for generating other KPIs. They are marked as `Parameter` in the table
below. Parameters are included to provide context on the simulation and allow
for sense-checking, whereas KPIs are intended to allow for measurement of sustainable mobility and be compared across scenarios.

## The KPI Metrics
We will add more KPIs as we go, but for now, these are the KPIs generated by Gelato:

| Themes                                                                | KPI Name                    | File                                                                         | Definition                                                                                                             | Methodology                                                                                                                                                                                                                          | Intermediate Data                                 |
|-----------------------------------------------------------------------|-----------------------------|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| <ul><li>Environment</li><li>Mobility System Performance</li></ul>     | Congestion                  | `kpi-congestion.csv.gz`                                                      | Delays in road traffic and in public transport during peak hours compared to free flow travel                          | Capture free-flow time at the link level, subtract congested time from this value. Congested time is the difference between link entry and exit time.                                                                                | `intermediate-congestion.csv.gz`                  |
| <ul><li>Social</li><li>Mobility System Performance</li></ul>          | PT Wait Time                | `kpi-pt-wait-time.csv.gz`                                                    | Average time waiting for a PT boarding                                                                                 | Average trip wait times by transport mode.                                                                                                                                                                                           | `intermediate-pt-wait-time.csv.gz`                |
| Parameter                                                             | Occupancy Rate              | `kpi-occupancy-rate.csv.gz`                                                  | Average load factor of vehicles of all modes                                                                           | Track boarding/alighting events at vehicle level and combine with the vehicle log to calculate distances, then aggregate to average occupancy by mode.                                                                               | `intermediate-occupancy-rate.csv.gz`              |
| Parameter                                                             | Vehicle KM                  | `kpi-vehicle-km.csv.gz`                                                      | Total distance travelled by all moving vehicles                                                                        | Sum the total distance travelled as recorded in the trip logs.                                                                                                                                                                       | `intermediate-vehicle-km.csv.gz`                  |
| Parameter                                                             | Speed                       | `kpi-speed.csv.gz`                                                           | Network link length divided by travel time                                                                             | Calculate average speed for each network link in hourly bins.                                                                                                                                                                        | None                                              |
| Parameter                                                             | Modal Split                 | `kpi-modal-split.csv.gz`                                                     | Modal split of dominant (by distance) trip modes                                                                       | Using trip logs, calculate the number of trips for each mode, as well as the percentage. This metric will not be scaled, but viewed in tandem with the other KPIs.                                                                   | None                                              |
| <ul><li>Quality of Life</li><li>Economic Success</li></ul>            | Travel Time                 | `kpi-travel-time.csv.gz`                                                     | Average travel time across all trips, in minutes                                                                       | Using trip logs, convert travel time to minutes, average across the trips.                                                                                                                                                           | `intermediate-travel-time.csv.gz`                 |
| <ul><li>Quality of Life</li><li>Mobility System Performance</li></ul> | Affordability               | `kpi-affordability.csv.gz`                                                   | Total cost of transit or drt trips for each agent                                                                      | Using leg logs and monetary scoring values per mode and person's subpopulation (per distance unit and constant), compute monetary cost for each leg. We report the total day cost per person for PT and DRT trips.                   | `intermediate-affordability.csv.gz`               |
| Quality of Life                                                       | Access to Mobility Services | `kpi-access-to-mobility-services-access-to-{bus,rail,pt-and-pt-used}.csv.gz` | Percentage of agents whose homes are within 400m of bus stops, 800m of rail stops, and whether they used PT for travel | From trip logs, we obtain agents' 'home' activity locations. We find whether there is a stop that serves transit services of modes 'bus' and 'rail', within distances of 400 and 800 metres of person's home location, respectively. | `intermediate-access-to-mobility-services.csv.gz` |

## Intermediate Data
Gelato generates a number of "intermediate" files that represent the intermediate, disaggregate output of
various KPI calculations:

| File                                              | Description                                                                                                                                                                 | Corresponding KPI File                                                       |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `intermediate-congestion.csv.gz`                  | Average delay ratio for travel time on each link, for each mode, across all hours of the modelled day, given the link was used within that hour by vehicle(s) of that mode. | `kpi-congestion.csv.gz`                                                      |
| `intermediate-occupancy-rate.csv.gz`              | Average occupancy of each vehicle, across the modelled day, given that vehicle has travelled. Capacity of the vehicle is reported for convenience.                          | `kpi-occupancy-rate.csv.gz`                                                  |
| `intermediate-pt-wait-time.csv.gz`                | Average waiting time, in seconds, for each transit stop and transit mode, across hours of the modelled day (given the transit stop was used for travel).                    | `kpi-pt-wait-time.csv.gz`                                                    |
| `intermediate-vehicle-km.csv.gz`                  | Total kilometres travelled by each vehicle during the modelled day. The mode of the vehicle is reported for convenience.                                                    | `kpi-vehicle-km.csv.gz`                                                      |
| `intermediate-travel-time.csv.gz`                 | Average travel time, in minutes, by trip purpose.                                                                                                                           | `kpi-travel-time.csv.gz`                                                     |
| `intermediate-affordability.csv.gz`               | Total monetary cost for all and each leg travelled for each agent. Agent's subpopulation is also reposted for convenience.                                                  | `kpi-travel-time.csv.gz`                                                     |
| `intermediate-access-to-mobility-services.csv.gz` | Home locations for each person, true/false columns to identify access to bus and rail stops, and whether the agent used transit.                                            | `kpi-access-to-mobility-services-access-to-{bus,rail,pt-and-pt-used}.csv.gz` |

## Supporting Data

In addition to KPI and intermediate files, Gelato also generates a number of "supporting data" files containing the
data aggregations used as inputs to the KPI calculations. These files allow the user to perform their own ad-hoc
analyses without having to parse and collate raw MATSim output data. Supporting data files use the naming scheme
`supporting-data-<name of aggregation>`.

| File                                                   | Description                                                                                                                                                                                                                                                                                                                              |
|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `supporting-data-legs.csv.gz`                          | Information (temporal and spatial) about the legs (subcomponents) of each trip of each person who travelled in the model (see: MATSim's `output_legs.csv` file).                                                                                                                                                                         |
| `supporting-data-linkLog.csv.gz`                       | A data table recording every vehicle's entry into and exit from each network link it travelled through, alongside the number of occupants in the vehicle at each of these points. Built by parsing raw link entry/exit events from MATSim's `output_events.xml.gz` and used to calculate KPIs such as `Congestion` and `Occupancy Rate`. |
| `supporting-data-networkLinkModes.csv.gz`              | Modes of transport allowed for each network link.                                                                                                                                                                                                                                                                                        |
| `supporting-data-networkLinks.csv.gz`                  | Links of the transport network, their speed, capacity, length (in metres) and number of lanes.                                                                                                                                                                                                                                           |
| `supporting-data-person-mode-score-parameters.csv.gz`  |                                                                                                                                                                                                                                                                                                                                          |
| `supporting-data-scheduleRoutes.csv.gz`                | Public transit routes and line IDs and their transit mode.                                                                                                                                                                                                                                                                               |
| `supporting-data-scheduleStops.csv.gz`                 | Public transit stops, their name, spatial coordinates, and the link of the network they are accessed by.                                                                                                                                                                                                                                 |
| `supporting-data-trips.csv.gz`                         | Information (temporal and spatial) about the trips of each person who travelled in the model (see: MATSim's `output_trips.csv` file).                                                                                                                                                                                                    |
| `supporting-data-vehicleOccupancy.csv.gz`              | Supporting table for the `supporting-data-linkLog.csv.gz`. Person IDs occupying the vehicle at the Link Log entry at the given index.                                                                                                                                                                                                    |
| `supporting-data-vehicles.csv.gz`                      | All vehicles, private and transit, known to the model, regardless of whether they have been used. For transit vehicles, transit Line and Route IDs are also reported.                                                                                                                                                                    |
