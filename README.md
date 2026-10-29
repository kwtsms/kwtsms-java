# kwtSMS Java Client

[![Tests](https://github.com/kwtsms/kwtsms-java/actions/workflows/test.yml/badge.svg)](https://github.com/kwtsms/kwtsms-java/actions/workflows/test.yml)
[![CodeQL](https://github.com/kwtsms/kwtsms-java/actions/workflows/codeql.yml/badge.svg)](https://github.com/kwtsms/kwtsms-java/actions/workflows/codeql.yml)
[![GitGuardian](https://github.com/kwtsms/kwtsms-java/actions/workflows/gitguardian.yml/badge.svg)](https://github.com/kwtsms/kwtsms-java/actions/workflows/gitguardian.yml)
[![JitPack](https://jitpack.io/v/kwtsms/kwtsms-java.svg)](https://jitpack.io/#kwtsms/kwtsms-java)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-blue)](https://www.oracle.com/java/)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-0-brightgreen)](https://github.com/kwtsms/kwtsms-java)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Java client for the [kwtSMS API](https://www.kwtsms.com). Send SMS, check balance, validate numbers, list sender IDs, check coverage, get delivery reports.

## About kwtSMS

kwtSMS is a Kuwaiti SMS gateway trusted by top businesses to deliver messages anywhere in the world, with private Sender ID, free API testing, non-expiring credits, and competitive flat-rate pricing. Secure, simple to integrate, built to last. Open a free account in under 1 minute, no paperwork or payment required. [Click here to get started](https://www.kwtsms.com/signup/)

## Prerequisites

You need **JDK 8+** to compile and run. Zero runtime dependencies.

### Step 1: Check if Java is installed

```bash
java -version
javac -version
```

If you see version numbers, you're ready. If not, install Java:

- **All platforms (recommended):** Download [Eclipse Temurin JDK](https://adoptium.net/) (free, LTS)
- **macOS:** `brew install temurin`
- **Ubuntu/Debian:** `sudo apt install default-jdk`
- **Windows:** Download installer from [adoptium.net](https://adoptium.net/)

### Step 2: Install kwtsms-java

**Gradle:**

```gradle
// settings.gradle
repositories {
    maven { url 'https://jitpack.io' }
}

// build.gradle
dependencies {
    implementation 'com.github.kwtsms:kwtsms-java:0.4.0'
}
```

**Gradle Kotlin DSL:**

```kotlin
// settings.gradle.kts
repositories {
    maven("https://jitpack.io")
}

// build.gradle.kts
dependencies {
    implementation("com.github.kwtsms:kwtsms-java:0.4.0")
}
```

**Maven:**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.kwtsms</groupId>
    <artifactId>kwtsms-java</artifactId>
    <version>0.4.0</version>
</dependency>
```

## Quick Start

```java
import com.kwtsms.*;

// Load credentials from environment variables or .env file
KwtSMS sms = KwtSMS.fromEnv();

// Or pass credentials directly
KwtSMS sms = new KwtSMS("java_your_api_user", "java_your_api_pass");

// Verify credentials
VerifyResult verify = sms.verify();
System.out.println("OK: " + verify.isOk() + ", Balance: " + verify.getBalance());

// Send SMS
SendResult result = sms.send("96598765432", "Hello from Java!");
System.out.println("Result: " + result.getResult());
System.out.println("Message ID: " + result.getMsgId());
System.out.println("Balance after: " + result.getBalanceAfter());
```

## Setup / Configuration

Create a `.env` file or set these environment variables:

```ini
KWTSMS_USERNAME=java_your_api_user
KWTSMS_PASSWORD=java_your_api_pass
KWTSMS_SENDER_ID=KWT-SMS
KWTSMS_TEST_MODE=1
KWTSMS_LOG_FILE=kwtsms.log
```

Or pass credentials directly:

```java
KwtSMS sms = new KwtSMS("java_your_api_user", "java_your_api_pass", "MY-SENDER", false, "kwtsms.log");
```

`KwtSMS.fromEnv()` reads environment variables first, falls back to `.env` file.

## Credential Management

**Never hardcode credentials.** Use one of these approaches:

1. **Environment variables / .env file** (default): `KwtSMS.fromEnv()` loads from env vars, then `.env` file. The file is `.gitignore`d and editable without redeployment.

2. **Spring Boot**: Set in `application.properties` or environment variables, load via `@Value` or `Environment`.

3. **Constructor injection**: `new KwtSMS(username, password, ...)` for custom config systems, DI containers, or remote config.

4. **Secrets manager**: Load from AWS Secrets Manager, HashiCorp Vault, Google Secret Manager, or your own config API, then pass to the constructor.

5. **Admin settings UI** (for web apps): Store credentials in your database with a settings page. Include a "Test Connection" button that calls `verify()`.

## All Methods

### Verify Credentials

```java
VerifyResult result = sms.verify();
if (result.isOk()) {
    System.out.println("Balance: " + result.getBalance());
} else {
    System.err.println("Error: " + result.getError());
}
```

### Send SMS

```java
// Single number
SendResult r = sms.send("96598765432", "Hello!");

// Multiple (comma-separated)
SendResult r = sms.send("96598765432,96512345678", "Hello!");

// Multiple (list)
SendResult r = sms.send(Arrays.asList("96598765432", "96512345678"), "Hello!");

// Custom sender ID
SendResult r = sms.send("96598765432", "Hello!", "MY-SENDER");

r.getResult();        // "OK" or "ERROR"
r.getMsgId();         // message ID (save this!)
r.getNumbers();       // count of numbers sent
r.getPointsCharged(); // credits deducted
r.getBalanceAfter();  // balance after send (save this!)
r.getCode();          // error code (e.g., "ERR003")
r.getDescription();   // error description
r.getAction();        // developer-friendly action message
r.getInvalid();       // list of numbers that failed local validation
```

### Send Bulk (>200 numbers)

```java
BulkSendResult r = sms.sendBulk(phoneList, "Campaign message");
r.getResult();        // "OK", "PARTIAL", or "ERROR"
r.getBatches();       // number of batches
r.getMsgIds();        // message IDs per batch
r.getErrors();        // per-batch errors
```

### Check Balance

```java
Double balance = sms.balance();           // live balance, cached fallback
Double cached = sms.getCachedBalance();   // from last verify/send
```

### Validate Numbers

```java
ValidateResult r = sms.validate(Arrays.asList("96598765432", "invalid"));
r.getOk();       // valid and routable numbers
r.getEr();       // format errors
r.getNr();       // no route (country not activated)
r.getRejected(); // failed local validation
```

### Sender IDs

```java
SenderIdResult r = sms.senderIds();
r.getSenderIds();  // ["KWT-SMS", "MY-APP"]
```

### Coverage

```java
CoverageResult r = sms.coverage();
r.getPrefixes();  // ["965", "966", ...]
```

### Message Status

```java
StatusResult r = sms.status("msg-id-from-send");
r.getStatus();             // "sent", "pending", etc.
r.getStatusDescription();  // human-readable status
```

### Delivery Report (international only)

```java
DeliveryReportResult r = sms.deliveryReport("msg-id-from-send");
for (DeliveryReportEntry entry : r.getReport()) {
    System.out.println(entry.getNumber() + ": " + entry.getStatus());
}
```

## Utility Functions

```java
import com.kwtsms.*;

// Normalize phone number (strips +, 00, spaces, dashes, converts Arabic digits, strips trunk prefix)
String phone = PhoneUtils.normalizePhone("+965 9876-5432");  // "96598765432"
String saudi = PhoneUtils.normalizePhone("9660559123456");   // "966559123456" (trunk 0 stripped)

// Validate phone input (generic + country-specific rules)
PhoneUtils.ValidationResult vr = PhoneUtils.validatePhoneInput("user@gmail.com");
// vr.isValid()=false, vr.getError()="'user@gmail.com' is an email address..."

PhoneUtils.ValidationResult vr2 = PhoneUtils.validatePhoneInput("96512345678");
// vr2.isValid()=false, vr2.getError()="Invalid Kuwait mobile number: after +965 must start with 4, 5, 6, 9"

// Find country code from a normalized number
String cc = PhoneUtils.findCountryCode("96598765432");  // "965"
String cc2 = PhoneUtils.findCountryCode("12025551234"); // "1"

// Validate against country-specific format rules (length + mobile prefix)
PhoneUtils.ValidationResult fmt = PhoneUtils.validatePhoneFormat("966559123456");  // valid Saudi
PhoneUtils.ValidationResult fmt2 = PhoneUtils.validatePhoneFormat("966712345678"); // invalid: must start with 5

// Deduplicate phone list
List<String> unique = PhoneUtils.deduplicatePhones(phoneList);

// Clean message text
String cleaned = MessageUtils.cleanMessage("Hello \uD83D\uDE00 OTP: \u0661\u0662\u0663");
// "Hello  OTP: 123"
```

## Input Sanitization

`MessageUtils.cleanMessage()` is called automatically by `send()` before every API call. It prevents the #1 cause of "message sent but not received" support tickets:

| Content | Effect without cleaning | What cleanMessage() does |
|---------|------------------------|--------------------------|
| Emojis | Stuck in queue, credits wasted, no error | Stripped |
| Hidden control characters (BOM, zero-width space, soft hyphen) | Spam filter rejection or queue stuck | Stripped |
| Arabic/Hindi numerals in body | OTP codes render inconsistently | Converted to Latin digits |
| HTML tags | ERR027, message rejected | Stripped |
| Directional marks (LTR, RTL) | May cause display issues | Stripped |

Arabic letters and Arabic text are fully supported and never stripped.

## Error Handling

Every ERROR response includes an `action` field with a developer-friendly fix:

```java
Map<String, String> errors = ApiErrors.API_ERRORS;
String action = errors.get("ERR003");
// "Wrong API username or password. Check KWTSMS_USERNAME and KWTSMS_PASSWORD..."

Map<String, Object> enriched = ApiErrors.enrichError(apiResponse);
// Adds "action" field with developer-friendly guidance
```

### User-facing error mapping

Raw API errors should never be shown to end users. Map them:

| Situation | API error | Show to user |
|-----------|----------|--------------|
| Invalid phone number | ERR006, ERR025 | "Please enter a valid phone number in international format (e.g., +965 9876 5432)." |
| Wrong credentials | ERR003 | "SMS service is temporarily unavailable. Please try again later." (log + alert admin) |
| No balance | ERR010, ERR011 | "SMS service is temporarily unavailable. Please try again later." (alert admin) |
| Country not supported | ERR026 | "SMS delivery to this country is not available." |
| Rate limited | ERR028 | "Please wait a moment before requesting another code." |
| Message rejected | ERR031, ERR032 | "Your message could not be sent. Please try again with different content." |
| Queue full | ERR013 | "SMS service is busy. Please try again in a few minutes." (library retries automatically) |
| Network error | Connection timeout | "Could not connect to SMS service." |

## Phone Number Formats

All formats are accepted and normalized automatically:

| Input | Normalized | Valid? |
|-------|-----------|--------|
| `96598765432` | `96598765432` | Yes |
| `+96598765432` | `96598765432` | Yes |
| `0096598765432` | `96598765432` | Yes |
| `965 9876 5432` | `96598765432` | Yes |
| `965-9876-5432` | `96598765432` | Yes |
| `(965) 98765432` | `96598765432` | Yes |
| `٩٦٥٩٨٧٦٥٤٣٢` | `96598765432` | Yes |
| `۹۶۵۹۸۷۶۵۴۳۲` | `96598765432` | Yes |
| `+٩٦٥٩٨٧٦٥٤٣٢` | `96598765432` | Yes |
| `٠٠٩٦٥٩٨٧٦٥٤٣٢` | `96598765432` | Yes |
| `٩٦٥ ٩٨٧٦ ٥٤٣٢` | `96598765432` | Yes |
| `٩٦٥-٩٨٧٦-٥٤٣٢` | `96598765432` | Yes |
| `965٩٨٧٦٥٤٣٢` | `96598765432` | Yes |
| `123456` (too short) | rejected | No |
| `user@gmail.com` | rejected | No |

## Test Mode

**Test mode** (`KWTSMS_TEST_MODE=1`) sends your message to the kwtSMS queue but does NOT deliver it to the handset. No SMS credits are consumed. Use this during development.

**Live mode** (`KWTSMS_TEST_MODE=0`) delivers the message for real and deducts credits. Always develop in test mode and switch to live only when ready for production.

## Sender ID

A **Sender ID** is the name that appears as the sender on the recipient's phone (e.g., "MY-APP" instead of a random number).

| | Promotional | Transactional |
|--|-------------|---------------|
| **Use for** | Bulk SMS, marketing, offers | OTP, alerts, notifications |
| **Delivery to DND numbers** | Blocked/filtered, credits lost | Bypasses DND (whitelisted) |
| **Speed** | May have delays | Priority delivery |
| **Cost** | 10 KD one-time | 15 KD one-time |

`KWT-SMS` is a shared test sender. It causes delivery delays, is blocked on Virgin Kuwait, and should never be used in production. Register your own private Sender ID through your kwtSMS account. For OTP/authentication messages, you need a **Transactional** Sender ID to bypass DND filtering. Sender ID is **case sensitive**.

## Best Practices

### Always save msg-id and balance-after

```java
SendResult r = sms.send(phone, message);
if ("OK".equals(r.getResult())) {
    db.save("sms_msg_id", r.getMsgId());         // needed for status/DLR
    db.save("sms_balance", r.getBalanceAfter());  // no extra API call needed
}
```

### Validate locally before calling the API

```java
PhoneUtils.ValidationResult vr = PhoneUtils.validatePhoneInput(userInput);
if (!vr.isValid()) {
    return error(vr.getError());  // rejected locally, no API call
}
```

### Country coverage pre-check

Call `coverage()` once at startup and cache the active prefixes. Before every send, check if the number's country prefix is in the list. If not, return an error immediately without hitting the API.

```java
// At startup
CoverageResult coverage = sms.coverage();
Set<String> activePrefixes = new HashSet<>(coverage.getPrefixes());

// Before send
if (!activePrefixes.stream().anyMatch(normalized::startsWith)) {
    return error("SMS delivery to this country is not available.");
}
```

### OTP requirements

- Always include app/company name: `"Your OTP for APPNAME is: 123456"`
- Resend timer: minimum 3-4 minutes (KNET standard is 4 minutes)
- OTP expiry: 3-5 minutes
- New code on resend: always generate a fresh code, invalidate previous
- Use Transactional Sender ID for OTP (not Promotional, not KWT-SMS)
- One number per OTP request: never batch OTP sends

### Thread safety

`KwtSMS` is thread-safe. Create one instance and share it across threads. Use it as a singleton bean in Spring.

## Security Checklist

Before going live:

- [ ] Bot protection enabled (CAPTCHA for web)
- [ ] Rate limit per phone number (max 3-5/hour)
- [ ] Rate limit per IP address (max 10-20/hour)
- [ ] Rate limit per user/session if authenticated
- [ ] Monitoring/alerting on abuse patterns
- [ ] Admin notification on low balance
- [ ] Test mode OFF (`KWTSMS_TEST_MODE=0`)
- [ ] Private Sender ID registered (not KWT-SMS)
- [ ] Transactional Sender ID for OTP (not promotional)

## What's Handled Automatically

- **Phone normalization**: `+`, `00`, spaces, dashes, dots, parentheses stripped. Arabic-Indic digits converted. Leading zeros removed.
- **Duplicate phone removal**: If the same number appears multiple times (in different formats), it is sent only once.
- **Message cleaning**: Emojis removed (codepoint-safe). Hidden control characters (BOM, zero-width spaces, directional marks) removed. HTML tags stripped. Arabic-Indic digits in message body converted to Latin.
- **Batch splitting**: More than 200 numbers are automatically split into batches of 200 with 0.5s delay between batches.
- **ERR013 retry**: Queue-full errors are automatically retried up to 3 times with exponential backoff (30s / 60s / 120s).
- **Error enrichment**: Every API error response includes an `action` field with a developer-friendly fix hint.
- **Credential masking**: Passwords are always masked as `***` in log files. Never exposed.
- **Balance caching**: Balance is cached from every `verify()` and `send()` response. `balance()` falls back to the cached value on API failure.

## Examples

See the [examples/](examples/) directory:

| # | Example | Description |
|---|---------|-------------|
| 00 | [Raw API](examples/00-raw-api/) | Call every kwtSMS endpoint directly — no library, no dependencies |
| 01 | [Basic Usage](examples/01-basic-usage/) | Verify credentials, send SMS, check balance |
| 02 | [OTP Flow](examples/02-otp-flow/) | Validate phone, send OTP with best practices |
| 03 | [Bulk SMS](examples/03-bulk-sms/) | Bulk send with >200 number batching |
| 04 | [Spring Endpoint](examples/04-spring-endpoint/) | REST API endpoint using Spring Boot |
| 05 | [Error Handling](examples/05-error-handling/) | All error paths, user-facing message mapping |
| 06 | [OTP Production](examples/06-otp-production/) | Production OTP: rate limiting, CAPTCHA, hashed storage |

## Testing

```bash
# Unit tests (no credentials needed)
./gradlew test

# Integration tests (real API, test mode, no credits consumed)
export JAVA_USERNAME=java_your_api_user
export JAVA_PASSWORD=java_your_api_pass
./gradlew test
```

## FAQ

**1. My message was sent successfully (result: OK) but the recipient didn't receive it. What happened?**

Check the **Sending Queue** at [kwtsms.com](https://www.kwtsms.com/login/). If your message is stuck there, it was accepted by the API but not dispatched. Common causes are emoji in the message, hidden characters from copy-pasting, or spam filter triggers. Delete it from the queue to recover your credits. Also verify that `test` mode is off (`KWTSMS_TEST_MODE=0`). Test messages are queued but never delivered.

**2. What is the difference between Test mode and Live mode?**

**Test mode** (`KWTSMS_TEST_MODE=1`) sends your message to the kwtSMS queue but does NOT deliver it to the handset. No SMS credits are consumed. Use this during development. **Live mode** (`KWTSMS_TEST_MODE=0`) delivers the message for real and deducts credits. Always develop in test mode and switch to live only when ready for production.

**3. What is a Sender ID and why should I not use "KWT-SMS" in production?**

A **Sender ID** is the name that appears as the sender on the recipient's phone (e.g., "MY-APP" instead of a random number). `KWT-SMS` is a shared test sender. It causes delivery delays, is blocked on Virgin Kuwait, and should never be used in production. Register your own private Sender ID through your kwtSMS account. For OTP/authentication messages, you need a **Transactional** Sender ID to bypass DND (Do Not Disturb) filtering.

**4. I'm getting ERR003 "Authentication error". What's wrong?**

You are using the wrong credentials. The API requires your **API username and API password**, NOT your account mobile number. Log in to [kwtsms.com](https://www.kwtsms.com/login/), go to Account, and check your API credentials. Also make sure you are using POST (not GET) and `Content-Type: application/json`.

**5. Can I send to international numbers (outside Kuwait)?**

International sending is **disabled by default** on kwtSMS accounts. [Log in to your kwtSMS account](https://www.kwtsms.com/login/) and add coverage for the country prefixes you need. Use `coverage()` to check which countries are currently active on your account. Be aware that activating international coverage increases exposure to automated abuse. Implement rate limiting and CAPTCHA before enabling.

## Timestamps

`unix-timestamp` values in API responses are in **GMT+3 (Asia/Kuwait)** server time, not UTC. Convert when storing or displaying.

## Help & Support

- **[kwtSMS FAQ](https://www.kwtsms.com/faq/)**: Answers to common questions about credits, sender IDs, OTP, and delivery
- **[kwtSMS Support](https://www.kwtsms.com/support.html)**: Open a support ticket or browse help articles
- **[Contact kwtSMS](https://www.kwtsms.com/#contact)**: Reach the kwtSMS team directly for Sender ID registration and account issues
- **[API Documentation (PDF)](https://www.kwtsms.com/doc/KwtSMS.com_API_Documentation_v41.pdf)**: kwtSMS REST API v4.1 full reference
- **[Best Practices](https://www.kwtsms.com/articles/sms-api-implementation-best-practices.html)**: SMS API implementation best practices
- **[Integration Test Checklist](https://www.kwtsms.com/articles/sms-api-integration-test-checklist.html)**: Pre-launch testing checklist
- **[kwtSMS Dashboard](https://www.kwtsms.com/login/)**: Recharge credits, buy Sender IDs, view message logs, manage coverage
- **[Other Integrations](https://www.kwtsms.com/integrations.html)**: Plugins and integrations for other platforms and languages

## License

[MIT](LICENSE)
