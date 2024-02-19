# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Dockerfile ([#11](https://github.com/arup-group/gelato/issues/11))
- Support for MATSim runIds
- Support for MATSim outputs with differing file compression types

### Fixed

- Error parsing trips & legs with large numbers of rows and one or more time columns with values > 24 hours ([#18](https://github.com/arup-group/gelato/issues/18))
- SLF4J runtime error/warning ([#20](https://github.com/arup-group/gelato/issues/20))
- drt mode not showing up in congestion outputs ([#10](https://github.com/arup-group/gelato/issues/10), [#23](https://github.com/arup-group/gelato/issues/23))


### Changed

- Output files are now compressed ([#27](https://github.com/arup-group/gelato/issues/27))

## [0.0.1-alpha] - 2024-02-07

The initial alpha pre-release. The functionality is as described in the
[pinned version of the README](https://github.com/arup-group/gelato/blob/b5d33fab229d1e2e55e3346a7b53d35be2b2ab09/README.md).
