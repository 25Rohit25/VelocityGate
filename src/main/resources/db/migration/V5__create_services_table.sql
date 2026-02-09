CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(500) NOT NULL,
    status VARCHAR(50) DEFAULT 'UNKNOWN',
    last_health_check TIMESTAMP,
    metadata TEXT
);

CREATE INDEX idx_services_name ON services(name);
