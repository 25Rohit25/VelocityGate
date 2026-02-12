# VelocityGate

[![Build Status](https://github.com/yourusername/VelocityGate/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/yourusername/VelocityGate/actions)
[![Code Coverage](https://codecov.io/gh/yourusername/VelocityGate/branch/main/graph/badge.svg)](https://codecov.io/gh/yourusername/VelocityGate)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Release](https://img.shields.io/github/v/release/yourusername/VelocityGate)](https://github.com/yourusername/VelocityGate/releases)

---

**A high-performance, distributed API Gateway built for scale.** <br/>
VelocityGate handles authentication, advanced rate limiting, and traffic management with sub-millisecond overhead, designed for cloud-native microservices.

---

## 🚀 Why VelocityGate?

Modern microservices demand resilience at the edge. VelocityGate solves three critical problems:

1.  **DDoS & Abuse**: Prevents system overload with distributed, algorithmic rate limiting (Token Bucket, Sliding Window).
2.  **Latency Spikes**: Fails fast and isolates failing backends using circuit breakers.
3.  **Observability Blindspots**: Provides real-time metrics on throughput, latency, and rejection rates out-of-the-box.

### Key Features

- **🛡️ Distributed Rate Limiting**: Redis-backed, atomic Lua scripts for 100% accuracy across clusters.
- **⚡ High Performance**: Non-blocking I/O (Spring WebFlux/Netty) handling 10k+ RPS per node.
- **🔌 Dynamic Configuration**: Update limits, quotas, and routes in real-time without restarts.
- **🔒 Enterprise Security**: JWT (RS256) validation, API Key hashing, and strict CORS policies.
- **📊 Deep Observability**: Native Prometheus metrics, Grafana dashboards, and ELK-ready JSON logs.
- **🌐 Kubernetes Ready**: Helm charts, Liveness probes, and HPA configurations included.

## 🏗️ Architecture

VelocityGate sits at the edge of your infrastructure, intercepting all ingress traffic.

_(See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed diagrams)_

```mermaid
graph LR
    Client -->|HTTPS| LB{Load Balancer}
    LB --> Gate[VelocityGate Cluster]
    Gate -->|Check Limit| Redis[(Redis Cluster)]

    Gate -- Allowed --> SvcA[User Service]
    Gate -- Allowed --> SvcB[Order Service]

    Gate -- Rejected --> 429[429 Too Many Requests]
```

## 🏎️ Quick Start

Get VelocityGate up and running in 30 seconds with Docker Compose.

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/VelocityGate.git
cd VelocityGate

# 2. Start all services (Gateway, Redis, Postgres, Prometheus, Grafana)
docker-compose up -d

# 3. Verify health
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## 📚 Documentation

- [📖 Deployment Guide (K8s/AWS)](DEPLOYMENT.md)
- [🧠 Distributed Rate Limiting Deep Dive](DISTRIBUTED_LIMITING.md) - How we solve race conditions with Lua.
- [📉 Observability & Dashboards](OBSERVABILITY.md) - Setting up Grafana & Alerts.
- [🛡️ Security Architecture](SECURITY.md) - Auth flows and secret management.
- [💥 Chaos Engineering Plan](chaos-tests/README.md) - How we validate resilience.
- [🔮 Future Roadmap](ROADMAP.md) - GraphQL, WASM, and more.

## 🧪 Performance

We validate performance with rigorous load testing (k6). _(See [PERFORMANCE_BENCHMARKS.md](PERFORMANCE_BENCHMARKS.md))_

| Scenario        | RPS    | Latency (P95) | Success Rate |
| :-------------- | :----- | :------------ | :----------- |
| **Baseline**    | 2,000  | 12ms          | 100%         |
| **High Load**   | 10,000 | 45ms          | 99.9%        |
| **Stress Test** | 25,000 | 180ms         | 99.5%        |

## 🛠️ API Reference

VelocityGate exposes a management API for configuring routes and limits.

**Get API Key Details**

```bash
curl -H "X-API-Key: vg_xyz123" http://localhost:8080/api/v1/auth/me
```

_Full OpenAPI/Swagger documentation available at `http://localhost:8080/swagger-ui.html`_

## 🤝 Contributing

We welcome contributions! Please check our [Contributing Guide](ROADMAP.md#4-contributing-guide) and [Code of Conduct](CODE_OF_CONDUCT.md).

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

_Built with ❤️ by [Your Name]_
