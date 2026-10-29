# Spring Boot Endpoint

Demonstrates integrating kwtsms-java into a Spring Boot REST API.

## Setup

Add to your `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.kwtsms:kwtsms-java:0.1.0'
}
```

## Key Points

- Create `KwtSMS` as a singleton bean (thread-safe, reuse across requests)
- Use `KwtSMS.fromEnv()` to load credentials from `application.properties` or environment variables
- Never expose raw API errors to end users: show generic messages, log the real error
- Add rate limiting middleware before SMS endpoints (prevent balance drain)
- Add CAPTCHA on forms that trigger SMS sends
