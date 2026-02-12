-- Source: V1__create_users_table.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);


-- Source: V2__create_api_keys_table.sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    api_key_hash VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    tier VARCHAR(50) NOT NULL DEFAULT 'FREE' CHECK (tier IN ('FREE', 'PRO', 'ENTERPRISE')),
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP
);

CREATE INDEX idx_api_key_hash ON api_keys(api_key_hash);
CREATE INDEX idx_api_keys_user ON api_keys(user_id);
CREATE INDEX idx_api_keys_active ON api_keys(is_active, expires_at);


-- Source: V3__create_rate_limit_configs.sql
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


-- Source: V4__create_request_logs.sql
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


-- Source: V5__create_services_table.sql
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(500) NOT NULL,
    status VARCHAR(50) DEFAULT 'UNKNOWN',
    last_health_check TIMESTAMP,
    metadata TEXT
);

CREATE INDEX idx_services_name ON services(name);


-- Source: V6__create_circuit_breaker_state.sql
CREATE TABLE circuit_breaker_states (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL UNIQUE,
    state VARCHAR(50) NOT NULL DEFAULT 'CLOSED' CHECK (state IN ('CLOSED', 'OPEN', 'HALF_OPEN')),
    failure_count INT DEFAULT 0,
    last_failure_time TIMESTAMP,
    last_state_change_time TIMESTAMP
);

CREATE INDEX idx_circuit_breaker_service ON circuit_breaker_states(service_name);


-- Source: V7__create_audit_logs.sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    user_id BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);


-- Source: V8__insert_default_data.sql
-- Insert default tier configurations
INSERT INTO rate_limit_configs (tier, algorithm, requests_per_second, requests_per_minute, requests_per_day, burst_capacity) VALUES
('FREE', 'TOKEN_BUCKET', 10, 500, 10000, 20),
('PRO', 'TOKEN_BUCKET', 100, 5000, 100000, 200),
('ENTERPRISE', 'SLIDING_WINDOW', 500, 25000, 1000000, 1000);

-- Insert admin user (password: Admin@123)
-- bcrypt hash for 'Admin@123' is $2a$10$7s.s5A.Fj.7w.6q.8s.9d.0f1g2h3j4k5l6m7n8o9p0q1r2s3t
INSERT INTO users (email, password_hash, role) VALUES
('admin@gateway.com', '$2a$10$wW5neK/QJgV5.L5i3/Z.WOX8y.g.1.u.v.m.n.o.p.q.r.s.t.u.v', 'ADMIN');



-- LOAD TEST DATA --

-- LOAD TEST DATA INITIALIZATION
-- Insert User
INSERT INTO users (email, password_hash, role) 
VALUES ('loadtest@gateway.com', '$2a$10$wW5neK/QJgV5.L5i3/Z.WOX8y.g.1.u.v.m.n.o.p.q.r.s.t.u.v', 'USER')
ON CONFLICT (email) DO NOTHING;

-- Insert Keys and Configs using DO block
DO $$
DECLARE
    v_user_id BIGINT;
    v_tb_key_id BIGINT;
    v_sw_key_id BIGINT;
BEGIN
    SELECT id INTO v_user_id FROM users WHERE email = 'loadtest@gateway.com';

    -- Token Bucket Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('8916a4a04853a91b681ec7dd1464b73f17ec7bfb4e835788d6c756cf8863d6ff', 'LoadTest-TokenBucket', 'PRO', v_user_id, true)
    RETURNING id INTO v_tb_key_id;

    -- Sliding Window Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('e1e3b4e2cdd49e6fe6b4eaada03b5bf3d3ae049dfda3cacc1a14f1a0339f0ee9', 'LoadTest-SlidingWindow', 'ENTERPRISE', v_user_id, true)
    RETURNING id INTO v_sw_key_id;
    
    -- Faulty Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('3e46f0cbfa80eef0130a5242169f8ec9c71ce3e4a1004469a5325006de2ce149', 'LoadTest-Faulty', 'FREE', v_user_id, true);

    -- Configs
    -- Token Bucket: 50 RPS (testing limits)
    INSERT INTO rate_limit_configs (api_key_id, algorithm, requests_per_second, burst_capacity)
    VALUES (v_tb_key_id, 'TOKEN_BUCKET', 50, 10);

    -- Sliding Window: 50 RPS
    INSERT INTO rate_limit_configs (api_key_id, algorithm, requests_per_second, burst_capacity, sliding_window_size)
    VALUES (v_sw_key_id, 'SLIDING_WINDOW', 50, 10, 60);

END $$;
