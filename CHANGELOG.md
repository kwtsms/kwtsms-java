# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Docs
- Test mode: test messages are queued but not delivered. Their credits are held until you delete them from the Sending Queue in your kwtsms.com account (previously documented as "no credits consumed").
- ERR028 action text no longer states a fixed 15-second wait. Duplicate SMS protection is opt-in per account and its window is set per account (10 seconds by default).

## [0.4.0] - 2026-03-13

### Added
- Country-specific phone validation: 75+ country rules (local length + mobile prefix)
- `PhoneUtils.findCountryCode()`: match country code from normalized number (3, 2, or 1 digit)
- `PhoneUtils.validatePhoneFormat()`: validate against country-specific format rules
- Trunk prefix stripping in `normalizePhone()` (e.g., 9660559... becomes 966559..., 97105x becomes 9715x)
- `PHONE_RULES` map with validation rules for GCC, Levant, Africa, Asia, Europe, Americas, Oceania
- `COUNTRY_NAMES` map for human-readable error messages
- GitGuardian secret scanning workflow
- Zero Dependencies badge in README

### Removed
- CLI tool (`CLI.java`, `CLIIntegrationTest.java`). Replaced by standalone [kwtsms-cli](https://github.com/boxlinknet/kwtsms-cli)
- `application` plugin and `mainClass` from build.gradle

## [0.3.0] - 2026-03-06

### Added
- Raw API example (00-raw-api) demonstrating all kwtSMS endpoints without the client library
- Examples 01-06: basic usage, OTP flow, bulk SMS, Spring endpoint, error handling, OTP production
- Auto-release workflow (one-click release via Actions UI)
- Auto-merge workflow for Dependabot patch/minor updates
- Stale issue/PR cleanup workflow
- `status()`: check message queue status
- `deliveryReport()`: delivery reports for international numbers

## [0.2.0] - 2026-03-06

### Changed
- All credential placeholder values prefixed with `java_` (e.g., `java_your_api_user`) so API requests are identifiable per language client
- Updated all docs and examples with `java_` prefixed placeholder values

### Updated
- actions/checkout v4 → v6
- actions/setup-java v4 → v5
- gradle/actions v4 → v5
- github/codeql-action v3 → v4

## [0.1.0] - 2026-03-06

### Added
- Initial release
- `KwtSMS` client class with constructor and `fromEnv()` factory
- `verify()`: test credentials and get balance
- `balance()`: get credits with cached fallback
- `send()`: send SMS to single or multiple numbers
- `sendBulk()`: auto-batching for >200 numbers with ERR013 retry
- `validate()`: validate phone numbers via API
- `senderIds()`: list sender IDs
- `coverage()`: list active country prefixes
- `status()`: check message queue status
- `deliveryReport()`: delivery reports for international numbers
- `PhoneUtils`: `normalizePhone()`, `validatePhoneInput()`, `deduplicatePhones()`
- `MessageUtils`: `cleanMessage()` (emoji, HTML, control chars, Arabic digit conversion)
- `ApiErrors`: complete error code map with developer-friendly action messages, `enrichError()`
- `EnvLoader`: `.env` file parser (never modifies system environment)
- JSONL logging with password masking
- Thread-safe: `volatile` cached balance fields
- Zero runtime dependencies (Java 8+ stdlib only)
- Unit tests (phone, message, errors, env loader)
- Integration tests (live API with test_mode, skipped without JAVA_USERNAME)
