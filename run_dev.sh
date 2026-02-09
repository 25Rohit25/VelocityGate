#!/bin/bash

echo "========================================================"
echo "   VelocityGate - Hybrid Run Script (Local + Docker)"
echo "========================================================"

# Ensure script stops on error
set -e

echo "1. Cleaning up existing Docker infrastructure (and resetting volumes to fix auth errors)..."
cd docker || { echo "Error: docker/ directory not found"; exit 1; }
docker-compose down -v

echo "2. Starting Infrastructure (Postgres, Redis, Prometheus, Grafana)..."
docker-compose up -d postgres redis prometheus grafana

echo "3. Waiting for database to initialize (15 seconds)..."
sleep 15

echo "4. Starting API Gateway Application (Local)..."
cd ..
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
