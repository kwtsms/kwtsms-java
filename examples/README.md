# Examples

Usage examples for the kwtsms-java library.

| # | Example | Description |
|---|---------|-------------|
| 00 | [Raw API](00-raw-api/) | Call every kwtSMS endpoint directly - no library, no dependencies |
| 01 | [Basic Usage](01-basic-usage/) | Send an SMS, check balance, verify credentials |
| 02 | [OTP Flow](02-otp-flow/) | One-time password implementation |
| 03 | [Bulk SMS](03-bulk-sms/) | Send to large recipient lists with auto-batching |
| 04 | [Spring Endpoint](04-spring-endpoint/) | REST API endpoint using Spring Boot |
| 05 | [Error Handling](05-error-handling/) | Handle all error codes gracefully |
| 06 | [OTP Production](06-otp-production/) | Production-ready OTP service with rate limiting and CAPTCHA |

## Running Examples

Examples are standalone Java files. Copy the code into your project and adjust the credentials.

All examples use `KwtSMS.fromEnv()` to load credentials from environment variables or a `.env` file:

```bash
export KWTSMS_USERNAME=java_your_api_user
export KWTSMS_PASSWORD=java_your_api_pass
export KWTSMS_SENDER_ID=YOUR-SENDER
export KWTSMS_TEST_MODE=1
```
