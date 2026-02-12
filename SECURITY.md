# Security Architecture & Best Practices

This document outlines the production-grade security measures implemented in VelocityGate, covering authentication, data protection, and infrastructure security.

## 1. Credential Management

Hardcoding credentials is strictily forbidden. VelocityGate supports hierarchical configuration loading suitable for containerized and cloud-native environments.

### 1.1 Environment Variables (Standard)

All sensitive keys must be injected as environment variables at runtime.

| Variable         | Description                    | Example                         |
| :--------------- | :----------------------------- | :------------------------------ |
| `DB_PASSWORD`    | PostgreSQL password            | `super_secret_db_pass`          |
| `REDIS_PASSWORD` | Redis auth token               | `secure_redis_token`            |
| `JWT_SECRET`     | Signing key for tokens         | `base64_encoded_256_bit_random` |
| `API_KEY_PEPPER` | Server-side secret for hashing | `random_pepper_string`          |

### 1.2 Kubernetes Secrets (Production)

In Kubernetes, secrets are mounted as environment variables or files.

```yaml
# deployment.yaml
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: gateway-secrets
        key: db-password
```

### 1.3 AWS Secrets Manager (Enterprise)

For dynamic secret rotation, we integrate with AWS Secrets Manager using the Spring Cloud AWS starter.

```java
// AWS Secrets Manager Integration
@Bean
public SecretsManagerClient secretsManagerClient() {
    return SecretsManagerClient.builder()
        .region(Region.US_EAST_1)
        .build();
}
```

---

## 2. Authentication & Authorization

### 2.1 Password Hashing (BCrypt)

User passwords are **never** stored in plain text. We use BCrypt with a work factor of 12.

- **Algorithm**: BCrypt
- **Cost Factor**: 12 (adjustable based on hardware)
- **Salt**: Randomly generated per user (handled by BCrypt)

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### 2.2 API Key Hashing (SHA-256 + Pepper)

API Keys are equivalent to passwords. Storing them plain-text allows an attacker with DB access to impersonate users.

- **Strategy**: `SHA-256(apiKey + global_pepper)`
- **Pepper**: A secret string stored in environment variables (NOT in the DB).
- **Comparison**: Constant-time comparison to prevent timing attacks.

```java
public static String secureHash(String key, String pepper) {
    return Hashing.sha256()
        .hashString(key + pepper, StandardCharsets.UTF_8)
        .toString();
}
```

### 2.3 JWT Strategy (RS256 vs HS256)

- **Current**: `HS256` (Symmetric) - Good for monolithic/internal speed.
- **Production Recommendation**: `RS256` (Asymmetric).
  - **Why?**: The Gateway (Private Key) signs tokens. Downstream services (Public Key) verify them without needing the secret. This prevents key leakage in microservices.

**Token Rotation Policy**:

- `Access Token`: 15 minutes TTL.
- `Refresh Token`: 7 days TTL, sliding window.
- **Revocation**: Refresh tokens are stored in DB/Redis and can be revoked instantly.

---

## 3. Network & Transport Security

### 3.1 TLS/SSL Enforcement

Production traffic must use HTTPS.

- **Redirect**: HTTP -> HTTPS redirect enabled at Load Balancer or Gateway level.
- **HSTS**: `Strict-Transport-Security` header enforced (max-age=31536000; includeSubDomains).

### 3.2 CORS Configuration

Restrict Cross-Origin Resource Sharing to known domains only.

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          "[/**]":
            allowedOrigins: "https://app.velocitygate.com"
            allowedMethods: "GET,POST,PUT,DELETE"
            allowedHeaders: "Authorization,Content-Type,X-API-Key"
```

---

## 4. Security Headers

VelocityGate injects standard security headers into every response to protect clients.

| Header                    | Value                | Purpose                          |
| :------------------------ | :------------------- | :------------------------------- |
| `X-Content-Type-Options`  | `nosniff`            | Prevents MIME-type sniffing.     |
| `X-Frame-Options`         | `DENY`               | Prevents Clickjacking.           |
| `X-XSS-Protection`        | `1; mode=block`      | Enables browser XSS filters.     |
| `Content-Security-Policy` | `default-src 'self'` | Mitigates XSS/Injection attacks. |

---

## 5. Rate Limiting Strategy (DoS Prevention)

### 5.1 Layered Defense

1.  **IP-based Limiting**: (WAF Level) Blocks malicious bots/scrapers before they hit the app.
2.  **API-Key Limiting**: (Gateway Level) Enforces business quotas (e.g., 100 RPS per user).

### 5.2 Failure Mode

- **Fail-Closed**: Ideally, if rate limiting fails, we should block traffic to protect the backend.
- **Fail-Open**: In some high-availability contexts, we allow traffic if Redis is down (See `RESILIENCE.md`).

---

## 6. Secure Implementation Snippets

### 6.1 Secure Configuration Loading

Loads the "Pepper" secret safely.

```java
@Component
public class SecurityConfig {
    @Value("${API_KEY_PEPPER}")
    private String apiKeyPepper;

    @PostConstruct
    public void validate() {
        if (apiKeyPepper == null || apiKeyPepper.length() < 32) {
            throw new IllegalStateException("API_KEY_PEPPER must be set and >32 chars!");
        }
    }
}
```

### 6.2 Secure Random API Key Generation

Uses `SecureRandom` instead of `Random`.

```java
public class SecureKeyGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generateKey() {
        byte[] buffer = new byte[32]; // 256 bits of entropy
        random.nextBytes(buffer);
        return "vg_" + encoder.encodeToString(buffer); // Prefix 'vg_' for identification
    }
}
```

### 6.3 Constant-Time Comparison

Prevents timing attacks when validating hashes.

```java
public boolean validateHash(String input, String expected) {
    return MessageDigest.isEqual(
        input.getBytes(StandardCharsets.UTF_8),
        expected.getBytes(StandardCharsets.UTF_8)
    );
}
```
