import time
import requests
import random
import math
import concurrent.futures
from datetime import datetime

# Configuration
GATEWAY_URL = "http://localhost:8080"
API_KEYS = ["key-free", "key-pro", "key-ent", "key-bot", "key-ddos"]
ROUTES = ["/api/v1/users", "/api/v1/orders", "/api/v1/products", "/api/v1/admin"]


def generate_noise():
    """Simulates background noise (constant, low-level traffic)"""
    return random.randint(5, 15)


def generate_spike(step, period=60):
    """Simulates a sudden traffic spike every 'period' steps"""
    if step % period == 0:
        return 500  # Massive spike
    return 0


def generate_sine_wave(step, amplitude=50, period=20):
    """Simulates daily traffic cycles"""
    return int(amplitude * (math.sin(2 * math.pi * step / period) + 1))


def call_api(api_key, route):
    try:
        if "ddos" in api_key:
            # Simulate high-rate attack (will trigger 429s)
            requests.get(
                f"{GATEWAY_URL}{route}", headers={"X-API-Key": api_key}, timeout=0.1
            )
        elif "bot" in api_key:
            # Simulate crawling bot (steady state)
            requests.get(f"{GATEWAY_URL}{route}", headers={"X-API-Key": api_key})
        else:
            # Normal user behavior
            time.sleep(random.uniform(0.01, 0.1))  # Human/App think time
            requests.get(f"{GATEWAY_URL}{route}", headers={"X-API-Key": api_key})
    except:
        pass  # Ignore errors (they are recorded by Prometheus)


def main():
    print("Starting Dashboard Traffic Generator...")
    print("Metrics will appear in Grafana shortly.")
    print("Press Ctrl+C to stop.")

    step = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
        while True:
            # Calculate load for this second
            load = generate_noise() + generate_sine_wave(step) + generate_spike(step)

            # Select random keys and routes weighted by "tier"
            tasks = []
            for _ in range(load):
                key = random.choice(API_KEYS)
                route = random.choice(ROUTES)

                # Introduce errors occasionally
                if random.random() < 0.05:  # 5% error rate injection
                    route = "/api/v1/non-existent"

                tasks.append(executor.submit(call_api, key, route))

            concurrent.futures.wait(tasks)

            if step % 5 == 0:
                print(f"Step {step}: Generated {load} requests against {GATEWAY_URL}")

            step += 1
            time.sleep(1)


if __name__ == "__main__":
    main()
