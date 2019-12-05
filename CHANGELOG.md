# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.4]
### Changed
- Upgrades to version 100.13.38 of DAML SDK
- Take in updated CLI and Config options, and reapply additional config for Fabric added on top of the defaults
- Use StandaloneIndexer and StandaloneIndex instead of ReferenceServer
- Update and correct various artifact dependencies
- Force specific version of jackson to avoid clashing resolution
- Include initial authentication support with AuthWildcard implementation
- Use H2 for index server
- Include MaxInboundMessageSize setting
- Named logger factory
- Include metrics registry

## [0.1.3]
### Changed
- Use postgres as ledger index database [#23](https://github.com/digital-asset/daml-on-x-example/issues/23)
An ephemeral database is created with every start of the server process and is deleted at exit. 
- Upgrade to 100.13.20 version of the DAML SDK


## [0.1.2]
### Changed
- Upgrade to DAML SDK 100.12.25
Version 100.12.25 fixes number of small issues. Stubs added for allocateParty and uploadPublicPackages, which will be implemented in the next version.

## [0.1.1]
### Changed
- Upgrade to DAML SDK 100.12.22 [#14](https://github.com/digital-asset/daml-on-x-example/issues/14)
The signature of the submitTransaction method in the WriteService has changed to return CompletionStage[SubmissionResult]
instead of a Unit, as a consequence the implementation had to be adopted accordingly.

## [0.1.0]
### Added
- Initial version of this project
