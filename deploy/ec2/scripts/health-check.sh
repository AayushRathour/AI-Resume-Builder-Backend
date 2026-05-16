#!/usr/bin/env bash
set -euo pipefail

check() {
  local name="$1"
  local url="$2"
  local code
  code="$(curl -s -o /dev/null -w "%{http_code}" "$url" || true)"
  if [[ "$code" == "200" ]]; then
    echo "PASS  $name  $url"
  else
    echo "FAIL  $name  $url  (http=$code)"
    return 1
  fi
}

FAILS=0

check "eureka-server" "http://localhost:8761/" || FAILS=$((FAILS+1))
check "api-gateway" "http://localhost:8080/actuator/health" || FAILS=$((FAILS+1))
check "auth-service" "http://localhost:8081/actuator/health" || FAILS=$((FAILS+1))
check "resume-service" "http://localhost:8082/api/actuator/health" || FAILS=$((FAILS+1))
check "section-service" "http://localhost:8083/api/actuator/health" || FAILS=$((FAILS+1))
check "template-service" "http://localhost:8084/api/actuator/health" || FAILS=$((FAILS+1))
check "ai-service" "http://localhost:8085/api/actuator/health" || FAILS=$((FAILS+1))
check "export-service" "http://localhost:8086/actuator/health" || FAILS=$((FAILS+1))
check "jobmatch-service" "http://localhost:8087/api/actuator/health" || FAILS=$((FAILS+1))
check "notification-service" "http://localhost:8088/api/actuator/health" || FAILS=$((FAILS+1))

if [[ "$FAILS" -gt 0 ]]; then
  echo "Health check failed for $FAILS service(s)."
  exit 1
fi

echo "All services healthy."

