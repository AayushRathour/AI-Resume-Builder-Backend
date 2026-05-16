#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="${1:-}"
RUN_USER="${2:-ubuntu}"

if [[ -z "$APP_ROOT" ]]; then
  echo "Usage: sudo bash deploy/ec2/scripts/install-systemd.sh <app-root> [run-user]"
  exit 1
fi

if [[ ! -d "$APP_ROOT" ]]; then
  echo "App root does not exist: $APP_ROOT"
  exit 1
fi

SCRIPT_PATH="$APP_ROOT/deploy/ec2/scripts/start-service.sh"
if [[ ! -f "$SCRIPT_PATH" ]]; then
  echo "Missing script: $SCRIPT_PATH"
  exit 1
fi

chmod +x "$SCRIPT_PATH"

declare -A SERVICE_DIRS=(
  ["resumeai-eureka-server"]="eureka-server"
  ["resumeai-api-gateway"]="api-gateway"
  ["resumeai-auth-service"]="auth-service"
  ["resumeai-resume-service"]="resume-service"
  ["resumeai-section-service"]="section-service"
  ["resumeai-template-service"]="template-service"
  ["resumeai-ai-service"]="ai-service"
  ["resumeai-export-service"]="export-service"
  ["resumeai-jobmatch-service"]="jobmatch-service"
  ["resumeai-notification-service"]="notification-service"
)

for UNIT_NAME in "${!SERVICE_DIRS[@]}"; do
  DIR_NAME="${SERVICE_DIRS[$UNIT_NAME]}"
  UNIT_FILE="/etc/systemd/system/${UNIT_NAME}.service"
  SERVICE_PATH="$APP_ROOT/$DIR_NAME"
  ENV_PATH="$SERVICE_PATH/.env"

  cat > "$UNIT_FILE" <<EOF
[Unit]
Description=$UNIT_NAME
After=network.target
Wants=network.target

[Service]
Type=simple
User=$RUN_USER
WorkingDirectory=$SERVICE_PATH
EnvironmentFile=-$ENV_PATH
ExecStart=$SCRIPT_PATH $SERVICE_PATH
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

  echo "Installed $UNIT_FILE"
done

systemctl daemon-reload
echo "systemd units installed and daemon reloaded."

