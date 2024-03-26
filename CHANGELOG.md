# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] -

### Added

- Scaling/Normalisation to values between `0` and `10` for metrics ([#69](https://github.com/arup-group/gelato/issues/69)): 
`Affordability`, `PT Wait Time`, `Occupancy`, `GHG`, `Travel Time`, `Access to Mobility services`, `Congestion`

### Fixed

- GHG Emissions KPI results where under-reported, by exaggerating number of people in per capita calculations ([#73](https://github.com/arup-group/gelato/issues/73))

### Changed

-

## [0.0.3-alpha] - 2024-03-07

### Added

- Passenger KM KPI ([#50](https://github.com/arup-group/gelato/issues/50))
- Affordability KPI ([#59](https://github.com/arup-group/gelato/issues/59))
- Greenhouse gases (GHG) emissions KPI ([#60](https://github.com/arup-group/gelato/issues/60))
- Average travel time KPI ([#61](https://github.com/arup-group/gelato/issues/61))
- Access to mobility services KPI ([#62](https://github.com/arup-group/gelato/issues/62))
- Parking space demand/mobility space usage KPI ([#63](https://github.com/arup-group/gelato/issues/63))
- More supporting data tables:
  - `supporting-data-activity-facilities.csv.gz`
  - `supporting-data-activities.csv.gz`
  - `supporting-data-person-mode-score-parameters.csv.gz`

### Fixed

- Vehicle KM KPI output being off by factor of 10 when converting units ([#49](https://github.com/arup-group/gelato/issues/49))

### Changed

- Docker image now accepts optional runtime JVM parameters ([#36](https://github.com/arup-group/gelato/issues/36))
- README now includes JVM memory tuning instructions ([#48](https://github.com/arup-group/gelato/issues/48))
- Gelato now uses less memory ([#52](https://github.com/arup-group/gelato/issues/52))
- Tables: 
  - transit stops now include mode of transport using the stop ([#62](https://github.com/arup-group/gelato/issues/62))
  - vehicles get fuel types and emission factors ([#60](https://github.com/arup-group/gelato/issues/60))
  - trips and legs have an additional column reporting monetary cost of the trip or leg ([#59](https://github.com/arup-group/gelato/issues/59))

## [0.0.2-alpha] - 2024-02-21

### Added

- Dockerfile ([#11](https://github.com/arup-group/gelato/issues/11))
- Support for MATSim runIds
- Support for MATSim outputs with differing file compression types

### Fixed

- Error parsing trips & legs with large numbers of rows and one or more time columns with values > 24 hours ([#18](https://github.com/arup-group/gelato/issues/18))
- SLF4J runtime error/warning ([#20](https://github.com/arup-group/gelato/issues/20))
- DRT mode not showing up in congestion outputs ([#10](https://github.com/arup-group/gelato/issues/10), [#23](https://github.com/arup-group/gelato/issues/23))
- Missing data in congestion KPI outputs that were caused by infinite speed links ([#38](https://github.com/arup-group/gelato/issues/38))
- DRT mode not showing up in PT wait time KPI output ([#45](https://github.com/arup-group/gelato/issues/45), [#46](https://github.com/arup-group/gelato/issues/46))

### Changed

- Output files are now compressed ([#27](https://github.com/arup-group/gelato/issues/27))
- Wording and content of the README ([#16](https://github.com/arup-group/gelato/issues/16))


## [0.0.1-alpha] - 2024-02-07

The initial alpha pre-release. The functionality is as described in the
[pinned version of the README](https://github.com/arup-group/gelato/blob/b5d33fab229d1e2e55e3346a7b53d35be2b2ab09/README.md).
