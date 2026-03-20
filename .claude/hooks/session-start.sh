#!/bin/bash
set -euo pipefail

# Only needed in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

echo "Installing mise..."
if ! command -v mise &>/dev/null && ! [ -x "$HOME/.local/bin/mise" ]; then
    MISE_VERSION=$(curl -fsSL "https://github.com/jdx/mise/releases/latest" -o /dev/null -w "%{url_effective}" | grep -oE 'v[0-9]+\.[0-9]+\.[0-9]+')
    curl -fsSL "https://github.com/jdx/mise/releases/download/${MISE_VERSION}/mise-${MISE_VERSION}-linux-x64.tar.gz" \
        | tar -xz -C /tmp
    mkdir -p "$HOME/.local/bin"
    cp /tmp/mise/bin/mise "$HOME/.local/bin/mise"
    echo "mise ${MISE_VERSION} installed"
fi
echo 'eval "$(~/.local/bin/mise activate bash)"' >> "${CLAUDE_ENV_FILE}"

echo "Installing Java via mise..."
# mise-java.jdx.dev is blocked in this environment; install GraalVM CE manually from GitHub
JAVA_VERSION="graalvm-community-25.0.2"
JAVA_INSTALL_DIR="$HOME/.local/share/mise/installs/java/${JAVA_VERSION}"
if [ ! -d "$JAVA_INSTALL_DIR" ]; then
    GRAALVM_TAG="jdk-25.0.2"
    GRAALVM_FILE="graalvm-community-jdk-25.0.2_linux-x64_bin.tar.gz"
    GRAALVM_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/${GRAALVM_TAG}/${GRAALVM_FILE}"
    echo "Downloading GraalVM CE 25.0.2 from GitHub..."
    curl -fL --progress-bar "$GRAALVM_URL" -o /tmp/graalvm-ce-25.tar.gz
    mkdir -p "$HOME/.local/share/mise/installs/java"
    tar -xzf /tmp/graalvm-ce-25.tar.gz -C /tmp/
    mv /tmp/graalvm-community-openjdk-25.0.2+10.1 "$JAVA_INSTALL_DIR"
    rm -f /tmp/graalvm-ce-25.tar.gz
    echo "GraalVM CE 25.0.2 installed to $JAVA_INSTALL_DIR"
else
    echo "GraalVM CE 25.0.2 already installed"
fi

echo "Configuring Maven for Claude Code remote environment..."

# Create Maven config directory
mkdir -p ~/.m2

# Extract proxy host and port from HTTPS_PROXY environment variable
# Expected format: http://user:pass@host:port or http://host:port
if [ -n "${HTTPS_PROXY:-}" ]; then
    # Extract host and port from proxy URL
    PROXY_HOST=$(echo "$HTTPS_PROXY" | sed -E 's|https?://([^:@]*:)?([^:@]*)@||' | sed -E 's|https?://||' | cut -d':' -f1)
    PROXY_PORT=$(echo "$HTTPS_PROXY" | sed -E 's|https?://([^:@]*:)?([^:@]*)@||' | sed -E 's|https?://||' | cut -d':' -f2 | cut -d'/' -f1)
    echo "Detected proxy: $PROXY_HOST:$PROXY_PORT"

    # Get no_proxy list
    NO_PROXY_HOSTS="${NO_PROXY:-localhost|127.0.0.1}"

    # Create Maven settings.xml with proxy configuration
    cat > ~/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <proxies>
        <proxy>
            <id>http-proxy</id>
            <active>true</active>
            <protocol>http</protocol>
            <host>${PROXY_HOST}</host>
            <port>${PROXY_PORT}</port>
            <nonProxyHosts>${NO_PROXY_HOSTS}</nonProxyHosts>
        </proxy>
        <proxy>
            <id>https-proxy</id>
            <active>true</active>
            <protocol>https</protocol>
            <host>${PROXY_HOST}</host>
            <port>${PROXY_PORT}</port>
            <nonProxyHosts>${NO_PROXY_HOSTS}</nonProxyHosts>
        </proxy>
    </proxies>
</settings>
EOF
    echo "Maven settings.xml created with proxy configuration"
else
    echo "No HTTPS_PROXY environment variable found, skipping proxy configuration"
fi

echo "Session start hook completed successfully"
