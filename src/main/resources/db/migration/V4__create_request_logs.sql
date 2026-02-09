CREATE TABLE request_logs (
    id BIGSERIAL,
    timestamp TIMESTAMP NOT NULL,
    api_key_hash VARCHAR(64),
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INT,
    response_time_ms BIGINT,
    downstream_service VARCHAR(100),
    client_ip VARCHAR(45),
    user_agent TEXT,
    was_rate_limited BOOLEAN DEFAULT false,
    error_message TEXT,
    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

-- Create initial partitions
CREATE TABLE request_logs_2025_02 PARTITION OF request_logs
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE request_logs_2025_03 PARTITION OF request_logs
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE request_logs_2026_01 PARTITION OF request_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE request_logs_2026_02 PARTITION OF request_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

CREATE TABLE request_logs_2026_03 PARTITION OF request_logs
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE INDEX idx_request_logs_timestamp ON request_logs(timestamp);
CREATE INDEX idx_request_logs_api_key ON request_logs(api_key_hash, timestamp);
CREATE INDEX idx_request_logs_path ON request_logs(path, timestamp);
CREATE INDEX idx_request_logs_status ON request_logs(status_code, timestamp);
