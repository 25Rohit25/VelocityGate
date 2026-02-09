CREATE TABLE rate_limit_configs (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT REFERENCES api_keys(id) ON DELETE CASCADE,
    tier VARCHAR(50),
    endpoint_pattern VARCHAR(255),
    algorithm VARCHAR(50) DEFAULT 'TOKEN_BUCKET' CHECK (algorithm IN ('TOKEN_BUCKET', 'SLIDING_WINDOW', 'FIXED_WINDOW', 'LEAKY_BUCKET')),
    requests_per_second INT,
    requests_per_minute INT,
    requests_per_hour INT,
    requests_per_day INT,
    burst_capacity INT,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(api_key_id, endpoint_pattern)
);

CREATE INDEX idx_rate_limit_tier ON rate_limit_configs(tier);
CREATE INDEX idx_rate_limit_api_key ON rate_limit_configs(api_key_id);
