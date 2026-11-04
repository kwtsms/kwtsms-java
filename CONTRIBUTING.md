# Contributing

## Development Setup

1. Clone the repository:
```bash
git clone https://github.com/kwtsms/kwtsms-java.git
cd kwtsms-java
```

2. Run tests:
```bash
gradle test
```

3. Run integration tests (requires API credentials):
```bash
export JAVA_USERNAME=java_your_api_user
export JAVA_PASSWORD=java_your_api_pass
gradle test
```

## Requirements

- Java 8+ (compile target)
- JDK 17+ (build/development)
- Gradle 8+

## Project Structure

```
src/main/java/com/kwtsms/   Source files
src/test/java/com/kwtsms/   Test files
examples/                    Usage examples
```

## Code Style

- Java 8 compatible: no `var`, no records, no text blocks
- Zero runtime dependencies: use `java.net.HttpURLConnection`, built-in JSON
- All public classes are `final` (immutable result types)
- Thread-safe: use `volatile` for shared state
- Methods never throw unchecked exceptions to callers (catch and return result types)

## Pull Request Checklist

- [ ] All tests pass (`gradle test`)
- [ ] New features include unit tests
- [ ] No new runtime dependencies added
- [ ] Java 8 compatible
- [ ] CHANGELOG.md updated

## Branch Naming

- `feature/description` for new features
- `fix/description` for bug fixes
- `docs/description` for documentation changes
