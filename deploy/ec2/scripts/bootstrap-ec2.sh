#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="${1:-$(pwd)}"
RUN_USER="${2:-ubuntu}"
SKIP_BUILD="${3:-false}"

SERVICES=(
  "eureka-server"
  "api-gateway"
  "auth-service"
  "resume-service"
  "section-service"
  "template-service"
  "ai-service"
  "export-service"
  "jobmatch-service"
  "notification-service"
)

UNIT_NAMES=(
  "resumeai-eureka-server"
  "resumeai-api-gateway"
  "resumeai-auth-service"
  "resumeai-resume-service"
  "resumeai-section-service"
  "resumeai-template-service"
  "resumeai-ai-service"
  "resumeai-export-service"
  "resumeai-jobmatch-service"
  "resumeai-notification-service"
)

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing required file: $path"
    exit 1
  fi
}

echo "Using APP_ROOT=$APP_ROOT"
echo "Using RUN_USER=$RUN_USER"

require_file "$APP_ROOT/mvnw"
require_file "$APP_ROOT/deploy/ec2/scripts/install-systemd.sh"
require_file "$APP_ROOT/deploy/ec2/scripts/health-check.sh"

chmod +x "$APP_ROOT/mvnw"
chmod +x "$APP_ROOT/deploy/ec2/scripts/install-systemd.sh"
chmod +x "$APP_ROOT/deploy/ec2/scripts/start-service.sh"
chmod +x "$APP_ROOT/deploy/ec2/scripts/health-check.sh"

echo "Step 1/5: preparing per-service .env files (if missing)"
for svc in "${SERVICES[@]}"; do
  template="$APP_ROOT/deploy/ec2/env/${svc}.env.example"
  target="$APP_ROOT/${svc}/.env"
  require_file "$template"
  if [[ ! -f "$target" ]]; then
    cp "$template" "$target"
    echo "Created: $target"
  else
    echo "Exists:  $target (kept)"
  fi
done

echo "Step 2/5: building services"
if [[ "$SKIP_BUILD" == "true" ]]; then
  echo "Skipping build because SKIP_BUILD=true"
else
  (
    cd "$APP_ROOT"
    ./mvnw -DskipTests package
  )
fi

echo "Step 3/5: installing systemd units"
sudo bash "$APP_ROOT/deploy/ec2/scripts/install-systemd.sh" "$APP_ROOT" "$RUN_USER"

echo "Step 4/5: enabling and starting services"
sudo systemctl enable "${UNIT_NAMES[@]}"
sudo systemctl daemon-reload

sudo systemctl restart resumeai-eureka-server
sleep 8
sudo systemctl restart resumeai-auth-service resumeai-resume-service resumeai-section-service resumeai-template-service resumeai-ai-service resumeai-export-service resumeai-jobmatch-service resumeai-notification-service
sleep 8
sudo systemctl restart resumeai-api-gateway

echo "Step 5/5: running health checks"
bash "$APP_ROOT/deploy/ec2/scripts/health-check.sh"

echo "Bootstrap complete."
echo "If health check fails, inspect logs with:"
echo "  sudo journalctl -u resumeai-api-gateway -u resumeai-auth-service -u resumeai-resume-service -u resumeai-section-service -u resumeai-template-service -u resumeai-ai-service -u resumeai-export-service -u resumeai-jobmatch-service -u resumeai-notification-service -u resumeai-eureka-server -f"

