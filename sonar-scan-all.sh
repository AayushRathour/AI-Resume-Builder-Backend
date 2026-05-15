#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════════════
#  ResumeAI — Per-Microservice SonarQube Scan Script (Linux/macOS/CI)
#  Usage: ./sonar-scan-all.sh [SONAR_TOKEN]
#  Example: ./sonar-scan-all.sh sqp_4682e3f9c7393c73b7ed046f6c60bbe626b550fb
# ═══════════════════════════════════════════════════════════════════════════════

set -e

SONAR_HOST="http://localhost:9000"
SONAR_TOKEN="${1:?ERROR: SonarQube token required. Usage: ./sonar-scan-all.sh YOUR_SONAR_TOKEN}"

SERVICES=(
    ai-service
    api-gateway
    auth-service
    eureka-server
    export-service
    jobmatch-service
    notification-service
    resume-service
    section-service
    template-service
)

echo ""
echo "══════════════════════════════════════════════════════"
echo "  ResumeAI Enterprise SonarQube Per-Service Scanner"
echo "══════════════════════════════════════════════════════"
echo ""

# ── PHASE 1: Full build ──
echo "[PHASE 1] Building all services..."
./mvnw clean verify
echo "[PHASE 1] Build complete."
echo ""

# ── PHASE 2: Per-service scans ──
echo "[PHASE 2] Running per-service SonarQube scans..."
echo ""

PASS=0
FAIL=0

for SERVICE in "${SERVICES[@]}"; do
    echo "────────────────────────────────────────────────"
    echo "  Scanning: ${SERVICE}"
    echo "────────────────────────────────────────────────"
    pushd "${SERVICE}" > /dev/null
    if ../mvnw sonar:sonar \
        -Dsonar.host.url="${SONAR_HOST}" \
        -Dsonar.login="${SONAR_TOKEN}"; then
        echo "[PASS] ${SERVICE} scan complete."
        ((PASS++))
    else
        echo "[FAIL] ${SERVICE} scan failed."
        ((FAIL++))
    fi
    popd > /dev/null
    echo ""
done

# ── PHASE 3: Frontend scan ──
echo "────────────────────────────────────────────────"
echo "  Scanning: FRONTEND (React)"
echo "────────────────────────────────────────────────"
pushd "../Frontend React ResumeAI" > /dev/null
if npx sonarqube-scanner \
    --define sonar.host.url="${SONAR_HOST}" \
    --define sonar.token="${SONAR_TOKEN}"; then
    echo "[PASS] Frontend scan complete."
    ((PASS++))
else
    echo "[FAIL] Frontend scan failed."
    ((FAIL++))
fi
popd > /dev/null
echo ""

# ── PHASE 4: Aggregator scan ──
echo "────────────────────────────────────────────────"
echo "  Scanning: ROOT AGGREGATOR"
echo "────────────────────────────────────────────────"
if ./mvnw sonar:sonar \
    -Dsonar.host.url="${SONAR_HOST}" \
    -Dsonar.login="${SONAR_TOKEN}"; then
    echo "[PASS] Aggregator scan complete."
    ((PASS++))
else
    echo "[FAIL] Aggregator scan failed."
    ((FAIL++))
fi

echo ""
echo "══════════════════════════════════════════════════════"
echo "  SCAN SUMMARY"
echo "══════════════════════════════════════════════════════"
echo "  Passed: ${PASS}"
echo "  Failed: ${FAIL}"
echo "  Dashboard: ${SONAR_HOST}"
echo "══════════════════════════════════════════════════════"
echo ""
