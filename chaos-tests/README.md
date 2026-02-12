# Chaos Engineering Plan for VelocityGate

This document details the Chaos Engineering strategy to validate VelocityGate's resilience under failure conditions. We use **Toxiproxy** to simulate network faults and resource constraints.

## 1. Test Scenarios & Hypotheses

| ID       | Scenario                                      | Injection Method                           | Expected Outcome                                                                          | Validation Metric                                                      |
| :------- | :-------------------------------------------- | :----------------------------------------- | :---------------------------------------------------------------------------------------- | :--------------------------------------------------------------------- |
| **C-01** | **Redis Unavailable** (Network Partition)     | Disable Toxiproxy upstream to Redis.       | Gateway logs connection error. Requests **bypass rate limiting** (Fail-Open) and succeed. | HTTP 200 OK percent > 99%<br/>Latency < 50ms                           |
| **C-02** | **High Redis Latency** (Degraded Performance) | Add 2000ms latency jitter to Redis proxy.  | Circuit Breaker **OPENS**. Requests fail fast or return fallback.                         | 503 Service Unavailable / Fallback<br/>P99 Latency < 100ms (Fast Fail) |
| **C-03** | **Redis Connection Flapping**                 | Toggle proxy enabled/disabled every 100ms. | Application reconnects automatically. Some errors might occur but recover quickly.        | Error Rate < 5% during flapping<br/>Recovery Time < 2s                 |
| **C-04** | **Thundering Herd** (Recovery)                | Spike load to 500 RPS after Redis restart. | System stabilizes without crashing. Redis memory grows linearly.                          | CPU Usage < 80%<br/>No OOM crashes                                     |
| **C-05** | **Bandwidth Constraint**                      | Limit Redis bandwidth to 1KB/s.            | Timeouts occur. Circuit Breaker should trip to protect threads.                           | Open Circuit Logs<br/>Active Threads < Max Pool                        |

---

## 2. Infrastructure Setup (Docker Compose)

The test environment isolates the failure domain using Toxiproxy.

```mermaid
graph LR
    TestRunner[Chaos Script / K6] -->|HTTP Traffic| Gateway[VelocityGate]
    Gateway -->|Redis Commands| Toxiproxy[Toxiproxy Container]
    Toxiproxy -->|Proxied Traffic| Redis[Redis Container]

    subgraph "Chaos Control Plane"
        TestRunner -.->|Inject Faults (API)| Toxiproxy
    end
```

### Components

- **VelocityGate**: Configured to connect to `toxiproxy:6379`.
- **Redis**: Standard image, isolated from Gateway network-wise.
- **Toxiproxy**: Shopify's proxy for simulating network conditions.
- **Prometheus**: Scrapes Gateway metrics during chaos.

---

## 3. Execution & Reporting

Tests are orchestrated via `run_chaos.py`. The script generates a report in the following format:

### Sample Report (Summary)

**Run ID**: `CHAOS-20231024-001`
**Scenario**: Redis Latency Injection (2000ms)

- **Baseline (No Fault)**:
  - RPS: 500
  - Success Rate: 100%
  - P95 Latency: 15ms
- **Fault Injection (00:05 - 00:25)**:
  - Fault: `latency` (2000ms)
  - Observed Behavior: Circuit Breaker State changed to **OPEN** at 00:07.
  - Success Rate: 0% (Fast Fail) / 100% (Fallback)
  - P95 Latency: 5ms (Fast Fail)
- **Recovery**:
  - Circuit State: **HALF-OPEN** at 00:35
  - Circuit State: **CLOSED** at 00:40
  - Full Recovery Time: 15s

### Grafana Dashboards

_(Placeholder for screenshots showing the dip in Redis commands and rise in Circuit Breaker Open state)_

---

## 4. Run the Chaos Suite

Use the provided Python script to execute the full suite.

```bash
# 1. Start Infrastructure
docker-compose -f chaos-tests/docker-compose.yml up -d

# 2. Setup Toxiproxy
# (Done automatically by script, or manually via API)

# 3. specific scenario
python3 chaos-tests/run_chaos.py --scenario redis-latency
```
