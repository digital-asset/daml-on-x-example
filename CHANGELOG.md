# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.2]
### Changed
- Upgrade to DAML SDK 100.12.25 [#14](https://github.com/digital-asset/daml-on-x-example/issues/14)
Version 100.12.25 fixes number of small issues. Stubs added for allocateParty and uploadPublicPackages, which will be implemented in the next version.

## [0.1.1]
### Changed
- Upgrade to DAML SDK 100.12.22 [#14](https://github.com/digital-asset/daml-on-x-example/issues/14)
The signature of the submitTransaction method in the WriteService has changed to return CompletionStage[SubmissionResult]
instead of a Unit, as a consequence the implementation had to be adopted accordingly.

## [0.1.0]
### Added
- Initial version of this project
