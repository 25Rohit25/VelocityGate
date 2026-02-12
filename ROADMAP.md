# Features & Roadmap

VelocityGate is designed not just for today's scale, but for future extensibility. This document outlines our current capabilities and the ambitious roadmap ahead.

## 1. Advanced Capabilities (Current)

### Dynamic Configuration

- **Zero-Downtime Updates**: Rate limits can be adjusted in real-time via the Admin API without restarting the Gateway. Changes propagate instantly to the Redis backend.
- **Per-Tenant & User Hierarchies**: Different limits apply based on subscription tier (Free vs. Enterprise) and individual user behavior.

### Traffic Management

- **Burst Handling**: Token Bucket algorithm allows brief traffic spikes (up to `capacity`) while smoothing long-term rates.
- **Quota Management**: Enforces strict daily/monthly quotas (e.g., "10,000 calls/month") separately from short-term rate limits.
- **Priority Queuing**: (In specific configurations) Premium users bypass standard queues during high load, ensuring SLA compliance.

---

## 2. Future Roadmap (Technical Ambition)

We are actively exploring these features to make VelocityGate a world-class edge solution.

- [ ] **GraphQL Federation Support**: Native handling of GraphQL queries, complexity analysis, and schema stitching at the gateway level.
- [ ] **gRPC Transcoding**: Automatic HTTP/JSON to gRPC conversion for high-performance backend communication.
- [ ] **Adaptive Rate Limiting**: Intelligent limits based on backend health signals (CPU/Memory of downstream services) rather than static configuration.
- [ ] **ML-Based Anomaly Detection**: Unsupervised learning models to detect and block DDoS patterns or credential stuffing attacks in real-time.
- [ ] **Multi-Region Active-Active**: Global rate limiting state synchronization using CRDTs (Conflict-free Replicated Data Types) for eventual consistency across geo-distributed clusters.
- [ ] **WASM Plugins**: Support for custom filters written in Rust/C++ via WebAssembly for ultra-low latency extensions.
- [ ] **Native Image Compilation**: Full GraalVM support for <100ms startup times and reduced memory footprint in serverless environments.

---

## 3. Optimization Opportunities

We have identified several low-hanging fruits for extreme performance tuning:

1.  **Redis Pipelining**: Batching rate limit checks for high-throughput scenarios to reduce network RTT.
2.  **L1 In-Memory Cache**: Implementing a local Caffeine cache for "hot" keys (e.g., public configs) to reduce Redis load by 90%.
3.  **Netty Zero-Copy**: Optimizing buffer management to reduce CPU usage during payload forwarding.
4.  **HTTP/3 (QUIC)**: Enabling next-gen protocol support for unreliable networks.

---

## 4. Contributing Guide

We welcome contributions! Please follow these guidelines to maintain enterprise quality.

### Getting Started

1.  **Fork & Clone**: `git clone https://github.com/yourusername/VelocityGate.git`
2.  **Setup**: Run `docker-compose up -d` to start dependencies (Redis, Postgres).
3.  **Build**: `mvn clean install`

### Standards

- **Code Style**: We follow Google Java Style Guide. Checkstyle runs on every build.
- **Testing**:
  - Unit Tests (JUnit 5 + Mockito) are mandatory for logic.
  - Integration Tests (Testcontainers) for data access layers.
  - Performance Tests (k6) for critical path changes.
- **Commit Messages**: Follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat: add adaptive limiting`, `fix: redis timeout handling`).

### Architecture Decision Records (ADRs)

Major architectural changes must be proposed via an ADR in `/docs/adr`. Explain the _Context_, _Decision_, and _Consequences_.
