import requests
import time
import subprocess
import json
import logging
import concurrent.futures
from datetime import datetime

# Configuration
GATEWAY_URL = "http://localhost:8080"
TOXIPROXY_API = "http://localhost:8474"
SCENARIOS = ["baseline", "redis_latency", "redis_down", "bandwidth_limit"]

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("ChaosTest")


def wait_for_service(url, timeout=60):
    start = time.time()
    while time.time() - start < timeout:
        try:
            resp = requests.get(f"{url}/actuator/health")
            if resp.status_code == 200 and resp.json().get("status") == "UP":
                return True
        except requests.ConnectionError:
            pass
        time.sleep(1)
    return False


def setup_toxiproxy():
    # Delete existing proxies
    requests.post(f"{TOXIPROXY_API}/reset")

    # Create Redis proxy
    proxy_config = {
        "name": "redis_proxy",
        "listen": "0.0.0.0:6379",
        "upstream": "redis:6379",
        "enabled": True,
    }
    resp = requests.post(f"{TOXIPROXY_API}/proxies", json=proxy_config)
    if resp.status_code == 201:
        logger.info("Toxiproxy configured: mapped :6379 -> redis:6379")
    else:
        logger.error(f"Failed to setup Toxiproxy: {resp.text}")


def inject_toxic(proxy_name, toxic_type, attributes):
    url = f"{TOXIPROXY_API}/proxies/{proxy_name}/toxics"
    payload = {"type": toxic_type, "attributes": attributes}
    resp = requests.post(url, json=payload)
    if resp.status_code == 200:
        logger.info(f"Injected toxic: {toxic_type} with {attributes}")
        return resp.json()["name"]
    logger.error(f"Failed to inject toxic: {resp.text}")
    return None


def remove_toxic(proxy_name, toxic_name):
    if not toxic_name:
        return
    requests.delete(f"{TOXIPROXY_API}/proxies/{proxy_name}/toxics/{toxic_name}")
    logger.info(f"Removed toxic: {toxic_name}")


def run_load(duration_sec=10, rps=10):
    success = 0
    fail = 0
    fallback = 0
    latencies = []

    end_time = time.time() + duration_sec

    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        futures = []
        while time.time() < end_time:
            # Simple simulation of constant load
            futures.append(
                executor.submit(
                    requests.get,
                    f"{GATEWAY_URL}/api/v1/users/1",
                    headers={"X-API-Key": "test-key"},
                )
            )
            time.sleep(1.0 / rps)

        for future in concurrent.futures.as_completed(futures):
            try:
                resp = future.result()
                latencies.append(resp.elapsed.total_seconds() * 1000)
                if resp.status_code == 200:
                    success += 1
                elif resp.status_code == 503:  # Circuit Breaker / Fallback
                    fallback += 1
                else:
                    fail += 1
            except Exception as e:
                fail += 1

    avg_latency = sum(latencies) / len(latencies) if latencies else 0
    return success, fail, fallback, avg_latency


def main():
    logger.info("Starting Chaos Test Suite...")

    # 1. Start Environment
    subprocess.run(
        ["docker-compose", "-f", "chaos-tests/docker-compose.yml", "up", "-d"],
        check=True,
    )
    if not wait_for_service(GATEWAY_URL):
        logger.error("Gateway didn't start in time. Aborting.")
        return

    setup_toxiproxy()

    results = []

    # 2. Baseline
    logger.info("--- Running Scenario: BASELINE ---")
    s, f, fb, lat = run_load(duration_sec=10, rps=20)
    results.append(
        {
            "scenario": "Baseline",
            "success": s,
            "fail": f,
            "fallback": fb,
            "latency": lat,
        }
    )
    logger.info(f"Baseline: Success={s}, Fail={f}, Latency={lat:.2f}ms")

    # 3. Redis Latency (Circuit Breaker Test)
    logger.info("--- Running Scenario: DISCO LATENCY (2000ms) ---")
    toxic_id = inject_toxic("redis_proxy", "latency", {"latency": 2000, "jitter": 100})
    s, f, fb, lat = run_load(duration_sec=15, rps=20)
    remove_toxic("redis_proxy", toxic_id)
    results.append(
        {
            "scenario": "Redis Latency",
            "success": s,
            "fail": f,
            "fallback": fb,
            "latency": lat,
        }
    )
    logger.info(
        f"Latency Test: Success={s}, Fail={f}, Fallback={fb} (Expected high Fallback/Fast Fail)"
    )

    # 4. Redis Down (Fail Open Test)
    logger.info("--- Running Scenario: REDIS DOWN ---")
    # Disable proxy entirely
    requests.post(f"{TOXIPROXY_API}/proxies/redis_proxy", json={"enabled": False})
    s, f, fb, lat = run_load(duration_sec=10, rps=20)
    # Re-enable
    requests.post(f"{TOXIPROXY_API}/proxies/redis_proxy", json={"enabled": True})
    results.append(
        {
            "scenario": "Redis Down",
            "success": s,
            "fail": f,
            "fallback": fb,
            "latency": lat,
        }
    )
    logger.info(
        f"Down Test: Success={s}, Fail={f} (Expected high Success if Fail-Open)"
    )

    # 5. Report
    report_file = f"chaos-tests/report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
    with open(report_file, "w") as f:
        f.write("# Chaos Test Report\n\n")
        f.write("| Scenario | Success | Fail | Fallback | Avg Latency (ms) |\n")
        f.write("|---|---|---|---|---|\n")
        for r in results:
            f.write(
                f"| {r['scenario']} | {r['success']} | {r['fail']} | {r['fallback']} | {r['latency']:.2f} |\n"
            )

    logger.info(f"Report generated: {report_file}")

    # Teardown (optional)
    # subprocess.run(["docker-compose", "-f", "chaos-tests/docker-compose.yml", "down"])


if __name__ == "__main__":
    main()
