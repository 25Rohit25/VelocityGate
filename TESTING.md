# Testing Strategy & Quality Assurance

VelocityGate enforces a high standard of code quality through a multi-layered testing strategy, aiming for >80% code coverage and robust integration verification.

## 1. Testing Pyramid

We adhere to the classic testing pyramid:

- **Unit Tests (70%)**: Fast, isolated tests for individual classes (Rate Limiters, Utilities, Services).
- **Integration Tests (20%)**: Verifies interaction between components and external infrastructure (Redis, Postgres) using **Testcontainers**.
- **E2E / Load Tests (10%)**: Validates system behavior under realistic traffic (k6, Chaos).

---

## 2. Unit Testing Structure

### Key Areas Covered

- **Algorithms**: Verify token bucket refill math, sliding window precision, and edge cases (e.g., negative tokens).
- **Security**: JWT signature validation, expiration handling, and API key hashing vectors.
- **Resilience**: Circuit breaker state transitions (CLOSED -> OPEN -> HALF-OPEN).

### Tools

- **JUnit 5**: Test runner.
- **Mockito**: Mocking external dependencies (Repositories, RedisTemplate).
- **Project Reactor Test**: `StepVerifier` for reactive stream assertions.

---

## 3. Integration Testing (Testcontainers)

We do not mock databases in integration tests. We use **Testcontainers** to spin up ephemeral Docker instances of Redis and PostgreSQL.

### Scenarios

- **Distributed Rate Limiting**: Spin up 2 Gateway instances (embedded) sharing one Redis container to verify lock contention and quota synchronization.
- **Data Persistence**: Verify API keys and User data survive restarts.
- **Failure Recovery**: Kill the Redis container mid-test and verify "Fail-Open" behavior.

---

## 4. Code Coverage & Quality Gates

### JaCoCo Configuration

We use JaCoCo to enforce coverage metrics. The build _fails_ if limits are not met.

| Metric              | Threshold |
| :------------------ | :-------- |
| **Line Coverage**   | 80%       |
| **Branch Coverage** | 70%       |
| **Missed Classes**  | 0         |

**Exclusions**: DTOs, Configuration classes, and Generated code (Lombok).

### Static Analysis

- **SpotBugs**: Scans for common bugs (NullPointer dereferences, resource leaks).
- **Checkstyle**: Enforces Google Java Style (indentation, naming conventions).
- **SonarQube**: Continuous inspection of code quality (optional integration).

---

## 5. How to Run Tests

### Standard Run

```bash
mvn clean verify
```

### Run Only Unit Tests

```bash
mvn test
```

### Generate Coverage Report

```bash
mvn jacoco:report
# View at target/site/jacoco/index.html
```
