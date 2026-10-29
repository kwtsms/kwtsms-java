# 00 - Raw API

Call every kwtSMS REST endpoint directly with `java.net.HttpURLConnection` — no client library, no dependencies. Copy-paste any section into your project.

## What this example covers

| # | Endpoint | URL | Purpose |
|---|----------|-----|---------|
| 1 | balance | `POST /API/balance/` | Verify credentials, check available credits |
| 2 | senderid | `POST /API/senderid/` | List sender IDs on your account |
| 3 | coverage | `POST /API/coverage/` | List active country prefixes |
| 4 | validate | `POST /API/validate/` | Check if phone numbers are valid and routable |
| 5 | send | `POST /API/send/` | Send SMS to a single number |
| 6 | send | `POST /API/send/` | Send SMS to multiple numbers (comma-separated) |
| 7 | status | `POST /API/status/` | Check message queue status by msg-id |
| 8 | dlr | `POST /API/dlr/` | Delivery report (international numbers only) |

## Prerequisites

- JDK 8 or higher
- A kwtSMS account with API credentials ([sign up free](https://www.kwtsms.com/signup/))

## Step 1: Configure credentials

Open `RawApi.java` and replace the credentials at the top:

```java
static final String USERNAME  = "your_api_username";   // your kwtSMS API username
static final String PASSWORD  = "your_api_password";   // your kwtSMS API password
static final String SENDER_ID = "KWT-SMS";             // your sender ID
static final String TEST_MODE = "1";                   // "1" = test, "0" = live
```

> Your API credentials are **not** your mobile number. Log in at [kwtsms.com](https://www.kwtsms.com/login/) and check Account > API.

## Step 2: Compile and run

```bash
cd examples/00-raw-api
javac RawApi.java
java RawApi
```

## Step 3: Read the output

Each endpoint prints the raw JSON response. Example output:

```
── 1. Verify credentials (balance) ──
Response: {"result":"OK","available":500.0,"purchased":1000.0}

── 2. Sender IDs ──
Response: {"result":"OK","senderid":["KWT-SMS","MY-APP"]}

── 3. Coverage ──
Response: {"result":"OK","coverage":["965"]}

── 4. Validate numbers ──
Response: {"result":"OK","mobile":{"OK":["96598765432"],"ER":["invalid"],"NR":["966558724477"]}}

── 5. Send SMS ──
   (TEST MODE — message queued but NOT delivered)
Response: {"result":"OK","msg-id":"f4c841adee210f31...","numbers":1,"points-charged":1,"balance-after":499.0,"unix-timestamp":1684763355}

── 7. Message status ──
Response: {"result":"ERROR","code":"ERR029","description":"Message does not exist"}

── 8. Delivery report (international only) ──
Response: {"result":"ERROR","code":"ERR019","description":"No reports found"}
```

## API rules

Every request follows the same pattern:

1. **Method**: Always `POST` (never GET — GET logs credentials in server logs)
2. **Content-Type**: `application/json`
3. **Accept**: `application/json`
4. **Auth**: `username` + `password` in the JSON body
5. **Base URL**: `https://www.kwtsms.com/API/`

## Phone number format

Numbers must be digits only, international format, no prefix:

| Input | Send as | Why |
|-------|---------|-----|
| `96598765432` | `96598765432` | Correct |
| `+96598765432` | `96598765432` | Strip `+` |
| `0096598765432` | `96598765432` | Strip `00` |
| `965 9876 5432` | `96598765432` | Strip spaces |
| Multiple numbers | `96598765432,96512345678` | Comma-separated, max 200 |

## Response format

**Success:**
```json
{"result":"OK", ...}
```

**Error:**
```json
{"result":"ERROR","code":"ERR003","description":"Authentication error, username or password are not correct."}
```

## Important fields to save from send response

| Field | Why |
|-------|-----|
| `msg-id` | Needed to call `/status/` and `/dlr/` later. If you don't save it, you can't check delivery. |
| `balance-after` | Your new credit balance. Save it so you don't need to call `/balance/` again. |
| `points-charged` | How many credits this send consumed. |
| `unix-timestamp` | Server time in **GMT+3** (Asia/Kuwait), not UTC. Convert when storing. |

## Common error codes

| Code | Meaning | Fix |
|------|---------|-----|
| ERR003 | Wrong username or password | Check API credentials (not your mobile number) |
| ERR006 | No valid numbers | Strip `+`, `00`, spaces — digits only |
| ERR007 | More than 200 numbers | Split into batches of 200 |
| ERR009 | Empty message | Provide message text |
| ERR010 | Zero balance | Recharge at kwtsms.com |
| ERR011 | Insufficient balance | Buy more credits |
| ERR025 | Invalid number format | Use international format (e.g., `96598765432`) |
| ERR028 | Same number too fast | Wait 15 seconds between sends to the same number |

Full error code reference: [API Documentation (PDF)](https://www.kwtsms.com/doc/KwtSMS.com_API_Documentation_v41.pdf)

## Test mode vs live mode

| | Test (`"test":"1"`) | Live (`"test":"0"`) |
|--|---------------------|---------------------|
| Message delivered? | No — queued only | Yes |
| Credits consumed? | No | Yes |
| Use for | Development, testing | Production |

Test messages appear in your [Sending Queue](https://www.kwtsms.com/login/). Delete them to keep the queue clean.

## Next steps

This example shows raw HTTP calls. For production use, consider the [kwtsms-java client library](https://github.com/kwtsms/kwtsms-java) which adds:

- Phone number normalization (Arabic digits, `+`/`00` prefix, spaces, dashes)
- Duplicate phone number removal
- Message cleaning (emoji removal, hidden character stripping)
- Auto-batching for >200 numbers with delay
- ERR013 queue-full retry with exponential backoff
- Error enrichment with developer-friendly action hints
- Credential masking in logs
- Balance caching
