# Production Scaling Guide

This document details the scaling characteristics, resource planning, and bottleneck analysis for deploying VelocityGate in a high-throughput production environment.

## 1. Scaling Analysis & Resource Sizing

### CPU vs. IO Characteristics

- **CPU-Bound Operations**: SSL Termination, JWT Verification (`RS256`), JSON Parsing, Request Routing.
  - _Scaling Strategy_: **Horizontal Auto-scaling (HPA)**. Add more Gateway pods as CPU usage rises.
- **IO-Bound Operations**: Redis Rate Limit Checks, Database Lookups (API Keys), Proxying to Backend.
  - _Scaling Strategy_: Optimization of Connection Pools, Non-blocking I/O (Reactor Netty).

### Pool Sizing Recommendations

| Component                       | Default   | Recommended (Prod) | Rationale                                                                              |
| :------------------------------ | :-------- | :----------------- | :------------------------------------------------------------------------------------- |
| **Netty Worker Threads**        | CPU Cores | `CPU Cores * 2`    | Handle high concurrent connections non-blockingly.                                     |
| **Redis Connections (Lettuce)** | Shared    | `max-active: 50`   | Lettuce is thread-safe; rarely need huge pools unless heavily pipelining.              |
| **DB Connections (HikariCP)**   | 10        | `20-50`            | Critical for API Key validation if not caching. Keep small to avoid DB saturation.     |
| **JVM Heap**                    | 25% RAM   | `2GB - 4GB`        | Enough for caching keys/configs. Gateway is stateless, so massive heaps aren't needed. |

---

## 2. Infrastructure Costs & Capacity Planning (AWS)

Estimates based on `us-east-1` pricing (On-Demand).

### Scenario A: Start-up (10k RPS)

- **Compute**: 3x `t3.medium` (2 vCPU, 4GB RAM) - $0.125/hr
- **State**: AWS ElastiCache (Redis) `cache.t3.micro` (Primary + Replica) - $0.034/hr
- **Database**: RDS `db.t3.micro` - $0.017/hr
- **Est. Monthly Cost**: ~$130

### Scenario B: Growth (100k RPS)

- **Compute**: 10x `c6g.large` (2 vCPU, 4GB RAM, ARM-based) - $0.68/hr
  - _Why Graviton?_ Java runs efficiently on ARM, ~20% cheaper.
- **State**: ElastiCache `cache.m6g.large` (Cluster Mode: 3 shards) - $0.48/hr
- **Est. Monthly Cost**: ~$900

### Scenario C: Hyper-Scale (1M RPS)

- **Compute**: 50x `c6g.2xlarge` (8 vCPU, 16GB RAM) - Auto-scaling group.
- **State**: Redis Cluster with 10 shards (`cache.r6g.xlarge`) to distribute key space/IOPS.
- **Network**: AWS PrivateLink to minimize NAT Gateway costs.
- **Est. Monthly Cost**: ~$8,000+

---

## 3. Bottleneck Identification & Tuning

### 🔴 Bottleneck: Redis CPU (Lua Scripts)

**Symptom**: High latency on rate limit checks; Redis `engine_cpu_utilization` > 80%.
**Cause**: Complex Lua scripts (e.g., Sliding Window with huge ZSETS) blocking the single Redis thread.
**Solutions**:

1.  **Sharding**: Enable Redis Cluster. Distribute keys (`{tenant}:rate_limit`) across slots.
2.  **Algorithm**: Switch from Sliding Window to Token Bucket (O(1) complexity).
3.  **Local Cache**: Enable in-memory `Caffeine` cache in Gateway for "Hot" keys (sacrifices strict consistency).

### 🔴 Bottleneck: Network Bandwidth

**Symptom**: `dropped_packets`, high p99 latency but low CPU.
**Solutions**:

1.  **Compression**: Enable GZIP/Brotli in `application.yml`.
2.  **HTTP/2**: Enable end-to-end HTTP/2 to multiplex connections.

### 🔴 Bottleneck: JVM Garbage Collection

**Symptom**: Periodic latency spikes (Stop-the-world pauses).
**Solutions**:

1.  **G1GC / ZGC**: Use modern collectors. `java -XX:+UseZGC -jar app.jar`.
2.  **Object Allocation**: Reduce allocation in hot paths (reuse buffers).

---

## 4. Kubernetes Deployment Architecture

We use a standard High Availability pattern:

- **Deployment**: Stateless Gateway pods.
- **HPA**: Scales on CPU (target 70%) or Custom Metric (RPS).
- **PodDisruptionBudget**: Ensures > 60% availability during node upgrades.
- **Affinity**: Anti-affinity to spread pods across Availability Zones.

_(See `k8s/` directory for full manifests)_

---

## 5. Scalability Validation (Test Results)

We validated the architecture using `k6` on a 3-node cluster.

### Experiment 1: Horizontal Scaling

| Pods  | Input RPS | Success Rate | P95 Latency | Verdict                      |
| :---- | :-------- | :----------- | :---------- | :--------------------------- |
| **1** | 2,000     | 100%         | 15ms        | Baseline                     |
| **1** | 4,000     | 85%          | 1500ms      | **Saturation** (CPU 98%)     |
| **3** | 6,000     | 100%         | 18ms        | **Linear Scaling Confirmed** |

### Experiment 2: Redis Cluster Sharding

- **Single Node**: Capped at ~25k ops/sec due to Lua script overhead.
- **3-Node Cluster**: Achieved ~70k ops/sec.
- **Conclusion**: Rate limiting logic scales linearly with Redis shards.
