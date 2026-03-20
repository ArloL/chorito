#!/bin/bash
set -euo pipefail

# Only needed in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

echo "Setting up Maven proxy..."

# Kill any stale proxy from a previous session (JWT token has rotated)
pkill -f "maven-proxy.py" 2>/dev/null || true

# Start local proxy in background; it reads HTTPS_PROXY for current session credentials
python3 "$CLAUDE_PROJECT_DIR/.claude/scripts/maven-proxy.py" \
    > /tmp/maven-proxy.log 2>&1 &
PROXY_PID=$!
echo "Maven proxy started (PID $PROXY_PID)"

# Write Maven settings.xml pointing to local unauthenticated proxy
mkdir -p "$HOME/.m2"
cat > "$HOME/.m2/settings.xml" << 'SETTINGS'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <proxies>
    <proxy>
      <id>local-auth-proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>127.0.0.1</host>
      <port>3128</port>
    </proxy>
    <proxy>
      <id>local-auth-proxy-http</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>127.0.0.1</host>
      <port>3128</port>
    </proxy>
  </proxies>
</settings>
SETTINGS

# Wait for proxy to be ready
sleep 2

# Verify proxy is listening
if python3 -c "import socket; s=socket.create_connection(('127.0.0.1', 3128), timeout=5); s.close()" 2>/dev/null; then
    echo "Maven proxy is ready on 127.0.0.1:3128"
else
    echo "WARNING: Maven proxy did not start in time, check /tmp/maven-proxy.log"
    cat /tmp/maven-proxy.log || true
fi
