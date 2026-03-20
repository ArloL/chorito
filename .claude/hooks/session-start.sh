#!/bin/bash
set -euo pipefail

# Only needed in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

echo "=== session-start env ==="
env | sort
echo "========================="

SCRIPTS_DIR="${CLAUDE_PROJECT_DIR}/.claude/scripts"

echo "Configuring Maven proxy..."
python3 "${SCRIPTS_DIR}/configure-maven-proxy.py"

export JAVA_HOME="${HOME}/.local/share/java/graalvm-community"
echo "export JAVA_HOME=${JAVA_HOME}" >> "${CLAUDE_ENV_FILE}"

echo "Installing Java..."
python3 "${SCRIPTS_DIR}/install-graalvm.py"

echo "export JAVA_TOOL_OPTIONS=\"\${JAVA_TOOL_OPTIONS} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts\"" >> "${CLAUDE_ENV_FILE}"
