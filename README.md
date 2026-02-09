# Distributed Rate Limiter & API Gateway

A high-performance, distributed API Gateway built with Spring Boot 3.2 and Spring Cloud Gateway 4.1.

## Features

- **Distributed Rate Limiting**: Token Bucket, Sliding Window, Fixed Window, Leaky Bucket algorithms backed by Redis.
- **Authentication**: API Key (hashed) and JWT support.
- **Resilience**: Circuit Breaker pattern with Resilience4j.
- **Monitoring**: Prometheus metrics, Grafana dashboards, and detailed Request Logging.
- **Scalability**: Stateless architecture, Dockerized, Kubernetes-ready.

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.9+

### Running Locally with Docker

1. **Build the project**:

   ```bash
   mvn clean package -DskipTests
   ```

2. **Start Infrastructure**:

   ```bash
   cd docker
   docker-compose up -d
   ```

3. **Access Services**:
   - API Gateway: `http://localhost:8080`
   - Prometheus: `http://localhost:9090`
   - Grafana: `http://localhost:3000` (admin/admin)

### API Usage

1. **Register Admin User** (Direct DB insert via migration or custom endpoint):
   - Default user: `admin@gateway.com`
   - Password: `Admin@123`

2. **Login to get JWT**:

   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@gateway.com", "password":"Admin@123"}'
   ```

3. **Generate API Key**:

   ```bash
   curl -X POST http://localhost:8080/api/v1/keys \
     -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{"name":"My App Key", "tier":"PRO"}'
   ```

4. **Make Rate Limited Request**:
   ```bash
   curl http://localhost:8080/api/v1/users/1 \
     -H "X-API-Key: sk_live_<YOUR_API_KEY>"
   ```

## Configuration

See `application.yml` for default settings.

- Rate Limiter Algorithm defaults to `TOKEN_BUCKET`.
- Default Redis prefix: `rate_limit:`

## Architecture

- **PostgreSQL**: Stores Users, API Keys, Configs, Logs.
- **Redis**: Stores active rate limit counters and cache.
- **Spring Cloud Gateway**: Handles routing and filtering.
