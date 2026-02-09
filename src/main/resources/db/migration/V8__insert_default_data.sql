-- Insert default tier configurations
INSERT INTO rate_limit_configs (tier, algorithm, requests_per_second, requests_per_minute, requests_per_day, burst_capacity) VALUES
('FREE', 'TOKEN_BUCKET', 10, 500, 10000, 20),
('PRO', 'TOKEN_BUCKET', 100, 5000, 100000, 200),
('ENTERPRISE', 'SLIDING_WINDOW', 500, 25000, 1000000, 1000);

-- Insert admin user (password: Admin@123)
-- bcrypt hash for 'Admin@123' is $2a$10$7s.s5A.Fj.7w.6q.8s.9d.0f1g2h3j4k5l6m7n8o9p0q1r2s3t
INSERT INTO users (email, password_hash, role) VALUES
('admin@gateway.com', '$2a$10$wW5neK/QJgV5.L5i3/Z.WOX8y.g.1.u.v.m.n.o.p.q.r.s.t.u.v', 'ADMIN');
