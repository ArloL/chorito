#!/usr/bin/env python3
"""Import system CA certs into the GraalVM JDK truststore."""

import os
import re
import subprocess
import sys
import tempfile

JAVA_VERSION = "graalvm-community-25.0.2"
CA_BUNDLE = "/etc/ssl/certs/ca-certificates.crt"

java_home = os.path.join(
    os.path.expanduser("~"),
    ".local", "share", "mise", "installs", "java", JAVA_VERSION,
)
keytool = os.path.join(java_home, "bin", "keytool")
cacerts = os.path.join(java_home, "lib", "security", "cacerts")

if not os.path.exists(keytool):
    print(f"keytool not found at {keytool}, skipping CA import")
    sys.exit(0)

with open(CA_BUNDLE) as f:
    content = f.read()

certs = re.findall(
    r"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----",
    content,
    re.DOTALL,
)

imported = 0
for i, cert in enumerate(certs):
    with tempfile.NamedTemporaryFile(mode="w", suffix=".pem", delete=False) as f:
        f.write(cert)
        tmpfile = f.name
    result = subprocess.run(
        [
            keytool, "-importcert", "-trustcacerts", "-noprompt",
            "-keystore", cacerts, "-storepass", "changeit",
            "-alias", f"system-ca-{i}", "-file", tmpfile,
        ],
        capture_output=True,
        text=True,
    )
    os.unlink(tmpfile)
    if result.returncode == 0:
        imported += 1

print(f"Imported {imported} of {len(certs)} system CA certs into JVM truststore")
