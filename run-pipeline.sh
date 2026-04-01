#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Jenkins Pipeline Simulation (Testcontainers)"
echo "=========================================="

echo ""
echo "=========================================="
echo "Stage 1: Build & Unit Tests"
echo "=========================================="

docker run --rm \
    -v "$SCRIPT_DIR:/app" \
    -w /app \
    maven:3.9-eclipse-temurin-17 \
    mvn clean test -q

echo "Unit tests completed successfully!"

echo ""
echo "=========================================="
echo "Stage 2: Integration Tests"
echo "=========================================="
echo "Testcontainers will automatically start Oracle XE"

docker run --rm \
    -v "$SCRIPT_DIR:/app" \
    -w /app \
    -v /var/run/docker.sock:/var/run/docker.sock \
    maven:3.9-eclipse-temurin-17 \
    mvn verify -DskipUnitTests=true

echo "Integration tests completed successfully!"

echo ""
echo "=========================================="
echo "Stage 3: Package"
echo "=========================================="

docker run --rm \
    -v "$SCRIPT_DIR:/app" \
    -w /app \
    maven:3.9-eclipse-temurin-17 \
    mvn package -DskipTests -q

echo "Package created successfully!"

echo ""
echo "=========================================="
echo "Pipeline completed successfully!"
echo "=========================================="
