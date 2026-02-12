import os
import hashlib
import glob


# 1. Generate Data Script Content
def get_hash(key):
    return hashlib.sha256(key.encode()).hexdigest()


tb_key = "tb-test-key"
sw_key = "sw-test-key"
faulty_key = "faulty-test-key"

tb_hash = get_hash(tb_key)
sw_hash = get_hash(sw_key)
faulty_hash = get_hash(faulty_key)

data_sql = f"""
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
    VALUES ('{tb_hash}', 'LoadTest-TokenBucket', 'PRO', v_user_id, true)
    RETURNING id INTO v_tb_key_id;

    -- Sliding Window Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('{sw_hash}', 'LoadTest-SlidingWindow', 'ENTERPRISE', v_user_id, true)
    RETURNING id INTO v_sw_key_id;
    
    -- Faulty Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('{faulty_hash}', 'LoadTest-Faulty', 'FREE', v_user_id, true);

    -- Configs
    -- Token Bucket: 50 RPS (testing limits)
    INSERT INTO rate_limit_configs (api_key_id, algorithm, requests_per_second, burst_capacity)
    VALUES (v_tb_key_id, 'TOKEN_BUCKET', 50, 10);

    -- Sliding Window: 50 RPS
    INSERT INTO rate_limit_configs (api_key_id, algorithm, requests_per_second, burst_capacity, sliding_window_size)
    VALUES (v_sw_key_id, 'SLIDING_WINDOW', 50, 10, 60);

END $$;
"""

# 2. Combine with migration scripts
migration_dir = "src/main/resources/db/migration"
files = sorted(glob.glob(os.path.join(migration_dir, "V*.sql")))

full_content = ""
for fpath in files:
    with open(fpath, "r") as f:
        full_content += f"-- Source: {os.path.basename(fpath)}\n"
        full_content += f.read() + "\n\n"

full_content += "\n-- LOAD TEST DATA --\n" + data_sql

output_path = "scripts/load-test-init.sql"
with open(output_path, "w") as f:
    f.write(full_content)

print(f"Created {output_path} with schema and test data.")
