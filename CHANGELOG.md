# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased] -

### Added

- New KPIs: Affordability, Passenger KM, GHG, Travel time , Access to mobility services, Mobility space usage
- More supporting data tables: facilities, activities and person scores

### Fixed


### Changed

- Docker image now accepts optional runtime JVM parameters ([#36](https://github.com/arup-group/gelato/issues/36))
- README now includes JVM memory tuning instructions ([#48](https://github.com/arup-group/gelato/issues/48))
- Gelato now uses less memory ([#52](https://github.com/arup-group/gelato/issues/52))
- Tables: 
  - transit stops now include mode of transport using the stop
  - vehicles get fuel types and emission factors
  - trips and legs have an additional column reporting monetary cost of the trip or leg

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
