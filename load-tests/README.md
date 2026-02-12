# VelocityGate Load Testing Guide

This directory contains resources to load test the VelocityGate API Gateway using JMeter and Docker.

## Prerequisites

- Docker and Docker Compose installed.
- PowerShell (to run the script) or Bash (if on Linux/Mac).

## Setup

The environment includes:

- **Postgres**: With test data (Users, API Keys, Rate Limit Configs).
- **Redis**: For rate limiting.
- **Mock Service (Wiremock)**: Simulating User and Order services.
- **Gateway**: Configured to use the above.

## Running the Test

Run the PowerShell script:

```powershell
.\run-load-test.ps1 -Users 50 -RampUp 10 -Duration 60
```

Arguments:

- `Users`: Number of concurrent threads (virtual users).
- `RampUp`: Time in seconds to start all threads.
- `Duration`: Test duration in seconds.

## Scaling to 20k RPS

Achieving 20,000 Requests Per Second (RPS) depends on the latency of the backend and the gateway overhead.
JMeter threads execute requests as fast as possible by default (zero think time).

**Formula**: `RPS = Threads / (Response Time in seconds)`

Example:

- If Gateway+Mock latency is 10ms (0.01s):
  - 1 Thread = 100 RPS
  - 200 threads = 20,000 RPS.

**Strategy to reach 20k RPS**:

1. Start with **50 users**. Check the HTML report for "Throughput".
2. Increase to **100 users**. Throughput should double.
3. Continue increasing users (e.g., 200, 500) until you reach 20k RPS or errors increase.
4. Note that if the Gateway is the bottleneck, increasing threads further will just increase latency, not RPS.

## Scenarios Tested

The `test-plan.jmx` includes:

1. **Token Bucket Rate Limiter**: Targets `/api/v1/users/1` with a limited API key.
   - Expect 429 errors when RPS > configured limit.
2. **Sliding Window Rate Limiter**: Targets `/api/v1/orders/1`.
3. **Circuit Breaker**: Targets `/api/v1/faulty/1`.
   - Wiremock simulates failures/delays. Checks if Gateway opens the circuit (Fast fail).

## Reports

After the run, open `load-tests/results/report/index.html` to view the Dashboard.
Graphs include:

- Response Time over Time
- Active Threads
- Response Codes per Second (Visualizes 200 vs 429 vs 503)
