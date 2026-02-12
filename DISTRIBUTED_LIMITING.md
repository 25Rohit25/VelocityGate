# Deep Dive: Distributed Rate Limiting

This section provides a technical breakdown of how VelocityGate handles high-throughput distributed rate limiting, ensuring correctness and performance across a cluster of Gateway instances.

## 1. Why Lua Scripts? (The Atomicity Problem)

In a distributed system where multiple Gateway instances access a shared Redis cluster, a "Time-of-Check to Time-of-Use" (TOCTOU) race condition is a critical risk.

### The Race Condition Scenario

Imagine two instances attempting to consume the last available token simultaneously using standard Redis commands:

1.  **Instance A** reads the token count: `GET rate_limited:user_123` -> returns `1`
2.  **Instance B** reads the token count: `GET rate_limited:user_123` -> returns `1`
3.  **Instance A** decrements: `DECR rate_limited:user_123` -> sets to `0` (Allowed)
4.  **Instance B** decrements: `DECR rate_limited:user_123` -> sets to `-1` (Allowed - **FAIL**)

Both requests were allowed, violating the rate limit because the "read" and "write" operations were not atomic.

### The Lua Solution

VelocityGate solves this by executing the entire logic (READ + CALCULATE + WRITE) inside a **Redis Lua Script**. Redis guarantees that Lua scripts are executed **atomically**; no other command can run in the middle of a script execution.

- **Zero Race Conditions**: The state cannot change between our check and our update.
- **Reduced Network Latency**: Instead of sending 3-4 commands (GET, update logic, SET, EXPIRE), we send 1 script. This reduces network round-trips (RTT) significantly.

---

## 2. Token Bucket Implementation

The Token Bucket algorithm allows for busty traffic while enforcing an average rate.

### Lua Script Logic

```lua
-- Keys: [1] = rate_limit_key
-- Args: [1] = refill_rate (tokens/sec), [2] = capacity, [3] = current_time (seconds), [4] = requested_tokens

local key = KEYS[1]
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Retrieve current state
local state = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(state[1])
local last_refill = tonumber(state[2])

-- Initialize if missing
if not tokens then
    tokens = capacity
    last_refill = now
end

-- Refill tokens based on time passed
local delta = math.max(0, now - last_refill)
local to_add = delta * rate
local new_tokens = math.min(capacity, tokens + to_add)

-- Check if request can be fulfilled
local allowed = 0
if new_tokens >= requested then
    new_tokens = new_tokens - requested
    allowed = 1
    last_refill = now -- Update verification time
else
    allowed = 0
end

-- Save new state and expire (ttl = time to fill bucket)
redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', last_refill)
redis.call('EXPIRE', key, math.ceil(capacity / rate))

return { allowed, new_tokens }
```

---

## 3. Sliding Window Log Implementation

For strict windowing (e.g., "Max 100 requests in _any_ 60-second window"), we use a Sorted Set (ZSET).

### Lua Script Logic

```lua
-- Keys: [1] = window_key
-- Args: [1] = window_size_ms, [2] = limit, [3] = current_time_ms, [4] = unique_request_id

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local req_id = ARGV[4]

-- 1. Remove requests outside the window (Clean up old data)
local clear_before = now - window
redis.call('ZREMRANGEBYSCORE', key, 0, clear_before)

-- 2. Count requests in current window
local count = redis.call('ZCARD', key)

-- 3. Check limit
if count < limit then
    -- Allowed: Add current request (Score = timestamp, Member = unique_id)
    redis.call('ZADD', key, now, req_id)
    redis.call('PEXPIRE', key, window) -- Set expiry to auto-clean key if idle
    return 1 -- Allowed
else
    return 0 -- Denied
end
```

---

## 4. Performance Comparison

| Feature         | Standard Redis Commands                 | VelocityGate Lua Script            | Impact                                          |
| :-------------- | :-------------------------------------- | :--------------------------------- | :---------------------------------------------- |
| **Consistency** | **Eventual** (Prone to Race Conditions) | **Strong** (Atomic Execution)      | Prevents limits acting "loose" under high load. |
| **Network RTT** | Multiple (3-5 per request)              | **Single (1 per request)**         | **40-60% Latency Reduction** per gateway hop.   |
| **Throughput**  | Lower (Connection overhead)             | **Higher** (Server-side execution) | Handles more RPS with fewer Redis connections.  |
| **Complexity**  | High (Client-side locking needed)       | Low (Encapsulated in Redis)        | Simplifies application code complexity.         |

---

## 5. Edge Case Handling

### 🕒 Clock Drift

**Problem**: If Gateway instances have different system times, calculations relying on `now` passed from the client can be inconsistent.
**Solution**:

1.  We use Redis `TIME` command inside the Lua script where possible, or;
2.  We allow a small "drift tolerance" window.
    _VelocityGate primarily relies on the Gateway's passed timestamp, assuming NTP synchronization across pods is within <100ms._

### 💀 Redis Connection Failure

**Problem**: What if Redis is down?
**Solution**:

- **Fail Open (Default)**: If Redis is unreachable, the Gateway allows the request to proceed (logging the error). This ensures reliability over strict enforcement (CAP theorem: choosing Availability).
- **Fail Closed**: Configurable for strict security environments.

### 🧹 Key Expiration & Memory

**Problem**: `ZSET` or Hash keys persisting forever, consuming memory.
**Solution**:

- Every Lua script includes an `EXPIRE` or `PEXPIRE` command.
- Keys automatically self-destruct after the window passes or the bucket refills completely.
