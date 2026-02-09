CREATE TABLE circuit_breaker_states (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL UNIQUE,
    state VARCHAR(50) NOT NULL DEFAULT 'CLOSED' CHECK (state IN ('CLOSED', 'OPEN', 'HALF_OPEN')),
    failure_count INT DEFAULT 0,
    last_failure_time TIMESTAMP,
    last_state_change_time TIMESTAMP
);

CREATE INDEX idx_circuit_breaker_service ON circuit_breaker_states(service_name);
