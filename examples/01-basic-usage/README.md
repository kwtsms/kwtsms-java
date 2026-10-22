# Basic Usage

Demonstrates sending an SMS, verifying credentials, and checking balance.

## Steps

1. Create a `.env` file with your credentials
2. Use `KwtSMS.fromEnv()` to load them automatically
3. Call `verify()` to test credentials
4. Call `send()` to send a message

## Key Points

- Always call `verify()` first to confirm credentials are valid
- Save `msgId` from the send response for status tracking and delivery reports
- Save `balanceAfter` from the send response to avoid extra API calls
- Use `testMode=true` during development (queued, not delivered; credits held until you delete it from the Sending Queue)
