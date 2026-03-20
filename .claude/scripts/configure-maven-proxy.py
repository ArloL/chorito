#!/usr/bin/env python3
"""Write ~/.m2/settings.xml with proxy config derived from HTTPS_PROXY."""

import os
import sys
from urllib.parse import urlparse

SETTINGS_XML = """\
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
            <host>{host}</host>
            <port>{port}</port>
            <nonProxyHosts>{non_proxy_hosts}</nonProxyHosts>
        </proxy>
        <proxy>
            <id>https-proxy</id>
            <active>true</active>
            <protocol>https</protocol>
            <host>{host}</host>
            <port>{port}</port>
            <nonProxyHosts>{non_proxy_hosts}</nonProxyHosts>
        </proxy>
    </proxies>
</settings>
"""

https_proxy = os.environ.get("HTTPS_PROXY", "")
if not https_proxy:
    print("No HTTPS_PROXY environment variable found, skipping proxy configuration")
    sys.exit(0)

parsed = urlparse(https_proxy)
host = parsed.hostname
port = parsed.port
if not host or not port:
    print(f"ERROR: could not parse host:port from HTTPS_PROXY={https_proxy!r}", file=sys.stderr)
    sys.exit(1)
port = str(port)

non_proxy_hosts = os.environ.get("NO_PROXY", "localhost|127.0.0.1")
print(f"Detected proxy: {host}:{port}")

settings_dir = os.path.join(os.path.expanduser("~"), ".m2")
os.makedirs(settings_dir, exist_ok=True)
settings_path = os.path.join(settings_dir, "settings.xml")

with open(settings_path, "w") as f:
    f.write(SETTINGS_XML.format(host=host, port=port, non_proxy_hosts=non_proxy_hosts))

print(f"Maven settings.xml created with proxy configuration")
