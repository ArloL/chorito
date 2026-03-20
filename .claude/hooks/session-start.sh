#!/bin/bash
set -euo pipefail

# Only needed in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

SCRIPTS_DIR="${CLAUDE_PROJECT_DIR}/.claude/scripts"

echo "Installing Java..."
JAVA_HOME=$(python3 "${SCRIPTS_DIR}/install-graalvm.py")
export JAVA_HOME

echo "Importing system CA certs into JVM truststore..."
python3 "${SCRIPTS_DIR}/import-cacerts.py"

echo "Configuring Maven proxy..."
python3 "${SCRIPTS_DIR}/configure-maven-proxy.py"
