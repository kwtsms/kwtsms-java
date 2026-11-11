# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.5.0] - 2026-11-06

### Added
- Client identifies itself with name and version; optional app name argument.

### Changed
- Package author and license holder are now kwtSMS.

## [0.4.0] - 2026-03-13

### Added
- Country-specific phone validation.
- Trunk prefix handling in phone normalization.
- Zero Dependencies badge in README.

### Removed
- Built-in CLI. Replaced by the standalone [kwtsms-cli](https://github.com/boxlinknet/kwtsms-cli).

## [0.3.0] - 2026-03-06

### Added
- Raw API example and examples 01-06.
- Auto-release and auto-merge workflows.
- `status()` and `deliveryReport()`.

## [0.2.0] - 2026-03-06

### Changed
- Credential placeholders prefixed with `java_` in docs and examples.

## [0.1.0] - 2026-03-06

### Added
- Initial release
- `KwtSMS` client with `verify()`, `balance()`, `send()`, `sendBulk()`, `validate()`, `senderIds()`, `coverage()`, `status()` and `deliveryReport()`
- Phone and message utilities
- Error code map with developer-friendly messages
- `.env` loader
- JSONL logging
- Zero runtime dependencies (Java 8+ stdlib only)
- Unit and integration tests
