import hashlib


def get_hash(key):
    return hashlib.sha256(key.encode()).hexdigest()


tb_key = "tb-test-key"
sw_key = "sw-test-key"
faulty_key = "faulty-test-key"

tb_hash = get_hash(tb_key)
sw_hash = get_hash(sw_key)
faulty_hash = get_hash(faulty_key)

sql_content = f"""
-- Load Test Data Initialization

-- 1. Create Load Test User
INSERT INTO users (email, password_hash, role) 
VALUES ('loadtest@gateway.com', '$2a$10$wW5neK/QJgV5.L5i3/Z.WOX8y.g.1.u.v.m.n.o.p.q.r.s.t.u.v', 'USER')
ON CONFLICT (email) DO NOTHING;

-- Get User ID
DO $$
DECLARE
    v_user_id BIGINT;
BEGIN
    SELECT id INTO v_user_id FROM users WHERE email = 'loadtest@gateway.com';

    -- 2. Insert API Keys (using calculated hashes)
    
    -- Token Bucket Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('{tb_hash}', 'LoadTest-TokenBucket', 'PRO', v_user_id, true);

    -- Sliding Window Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('{sw_hash}', 'LoadTest-SlidingWindow', 'ENTERPRISE', v_user_id, true);
    
    -- Faulty/Circuit Breaker Key
    INSERT INTO api_keys (api_key_hash, name, tier, user_id, is_active)
    VALUES ('{faulty_hash}', 'LoadTest-Faulty', 'FREE', v_user_id, true);

    -- 3. Insert Rate Limit Configs
    
    -- Token Bucket Config (ID derived later or assume sequence - better to use subqueries or DO block)
    -- We need the API Key IDs.
    
    INSERT INTO rate_limit_configs (api_key_id, algorithm, requests_per_second, burst_capacity)
    SELECT id, 'TOKEN_BUCKET', 50, 10
    FROM api_keys WHERE api_key_hash = '{tb_hash}';

    INSERT INTO rate_limit_configs (api_key_id, algorithm, requests_per_second, burst_capacity)
    SELECT id, 'SLIDING_WINDOW', 50, 10
    FROM api_keys WHERE api_key_hash = '{sw_hash}';
    
    -- Circuit Breaker uses the endpoint pattern.
    -- We can set a specific limit for the faulty endpoint too if we want, but usually CB is distinct.
    
END $$;
"""

with open("scripts/init-load-test.sql", "w") as f:
    f.write(sql_content)

print("Created scripts/init-load-test.sql")
