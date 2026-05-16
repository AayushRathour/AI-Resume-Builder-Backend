#!/usr/bin/env bash
set -euo pipefail

SERVICE_DIR="${1:-}"
if [[ -z "$SERVICE_DIR" ]]; then
  echo "Usage: start-service.sh <service-dir>"
  exit 1
fi

if [[ ! -d "$SERVICE_DIR" ]]; then
  echo "Service directory not found: $SERVICE_DIR"
  exit 1
fi

JAR_PATH="$(find "$SERVICE_DIR/target" -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" ! -name "*sources.jar" ! -name "*javadoc.jar" | head -n 1)"
if [[ -z "$JAR_PATH" ]]; then
  echo "No runnable jar found in $SERVICE_DIR/target"
  exit 1
fi

exec /usr/bin/java -jar "$JAR_PATH"

