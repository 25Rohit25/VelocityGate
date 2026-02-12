# VelocityGate Grafana Dashboards

This document details the configuration, layout, and interpretation of the VelocityGate observability dashboards. These visualizations are critical for monitoring system health, debugging latency issues, and verifying rate limiting behavior.

## 1. Dashboard Layout Strategy

We follow the "Red Method" (Rate, Errors, Duration) combined with Rate Limiting specifics.

| Row       | Focus Area                              | Panels Included                                                       |
| :-------- | :-------------------------------------- | :-------------------------------------------------------------------- |
| **Top**   | **Key Health Indicators (Stat Panels)** | Current RPS, Error Rate %, Avg Latency (ms), Active Circuit Breakers. |
| **Row 1** | **Traffic & Limits (Time Series)**      | Total Request Volume vs. Rate Limit Rejections (429s).                |
| **Row 2** | **Performance (Time Series)**           | Latency Distribution (P50, P95, P99), Redis Command Latency.          |
| **Row 3** | **Detailed Breakdown (Bar/Pie)**        | Top API Consumers (Who is hitting us?), Errors by Route.              |
| **Row 4** | **Infrastructure (Gauges)**             | JVM Heap Usage, Redis Connection Pool Saturation.                     |

---

## 2. PromQL Query Reference

Use these queries when building or customizing panels.

### 2.1 Key Metrics

- **Total Requests Per Second (Throughput)**:
  ```promql
  sum(rate(gateway_requests_total[1m]))
  ```
- **Global Error Rate (%)**:
  ```promql
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))
  /
  sum(rate(http_server_requests_seconds_count[1m])) * 100
  ```
- **Avg Request Latency**:
  ```promql
  rate(http_server_requests_seconds_sum[1m])
  /
  rate(http_server_requests_seconds_count[1m])
  ```

### 2.2 Rate Limiting & Resilience

- **Rejection Rate (429s)**:

  ```promql
  sum(rate(gateway_ratelimit_rejected_total[1m]))
  ```

  _Useful to correlate with specific "noisy neighbor" API keys._

- **Circuit Breaker State**:
  ```promql
  max(resilience4j_circuitbreaker_state{state="open"}) by (name)
  ```
  _Returns 1 if Open, 0 if Closed/Half-Open._

### 2.3 Deep Dives

- **Top 5 API Consumers (by Request Volume)**:
  ```promql
  topk(5, sum(rate(gateway_requests_total[5m])) by (api_key_id))
  ```
- **Redis Latency Spike Detection**:
  ```promql
  rate(lettuce_command_latency_seconds_sum[1m])
  /
  rate(lettuce_command_latency_seconds_count[1m])
  ```

---

## 3. Interpreting the Data

### Normal vs. Concerning Values

| Metric               | Normal Range | Warning Sign ⚠️           | Critical Action 🚨                   |
| :------------------- | :----------- | :------------------------ | :----------------------------------- |
| **RPS**              | 0 - 5000     | Sudden 2x spike (DDoS?)   | -                                    |
| **Error Rate**       | < 0.1%       | > 1% (Backend issues)     | > 5% (System outage)                 |
| **Latency P99**      | < 100ms      | 100ms - 500ms             | > 1s (Circuit breakers will trip)    |
| **Circuit Breakers** | 0 Open       | Occasional Half-Open      | Persistent Open (Service Down)       |
| **Redis Latency**    | < 1ms        | > 5ms (Network/CPU issue) | > 20ms (Will cause Gateway timeouts) |

### Debugging Workflow

1.  **Alert Triggered**: "High Error Rate" alert fires.
2.  **Check Top Row**: Is it 5xx errors or just high latency?
3.  **Check Row 1 (Rejections)**: Is it actually valid traffic being rate limited (429s)? If so, check "Top Consumers".
4.  **Check Row 3 (Breakdown)**: Is the error isolated to one route (e.g., `/users`)?
    - _If yes_: Check Circuit Breaker panel.
    - _If no_: Check Redis Latency (Shared dependency issue).

---

## 4. Visualization Configuration (JSON Snippets for Grafana)

**Panel: P99 Latency with Thresholds**

```json
{
  "datasource": "Prometheus",
  "type": "timeseries",
  "title": "99th Percentile Latency",
  "targets": [
    {
      "expr": "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))",
      "legendFormat": "P99"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "thresholds": {
        "mode": "absolute",
        "steps": [
          { "value": null, "color": "green" },
          { "value": 0.2, "color": "orange" },
          { "value": 1.0, "color": "red" }
        ]
      },
      "unit": "s"
    }
  }
}
```

**Panel: Circuit Breaker State History**

```json
{
  "type": "state-timeline",
  "title": "Circuit Breaker History",
  "targets": [
    {
      "expr": "resilience4j_circuitbreaker_state",
      "legendFormat": "{{name}} - {{state}}"
    }
  ]
}
```
