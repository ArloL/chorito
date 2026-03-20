#!/usr/bin/env python3
"""Import system CA certs into the GraalVM JDK truststore."""

import os
import subprocess
import sys
import tempfile

CA_BUNDLE = "/etc/ssl/certs/ca-certificates.crt"

java_home = os.environ["JAVA_HOME"]
keytool = os.path.join(java_home, "bin", "keytool")
cacerts = os.path.join(java_home, "lib", "security", "cacerts")

if not os.path.exists(keytool):
    print(f"keytool not found at {keytool}, skipping CA import")
    sys.exit(0)

already_imported = subprocess.run(
    [keytool, "-list", "-keystore", cacerts, "-storepass", "changeit", "-alias", "system-ca-0"],
    capture_output=True,
).returncode == 0
if already_imported:
    print("System CA certs already imported, skipping.")
    sys.exit(0)

with tempfile.TemporaryDirectory() as tmpdir:
    p12 = os.path.join(tmpdir, "system-ca.p12")

    # Convert PEM bundle to PKCS12 in two openssl calls
    crl2pkcs7 = subprocess.run(
        ["openssl", "crl2pkcs7", "-nocrl", "-certfile", CA_BUNDLE],
        capture_output=True,
        check=True,
    )
    subprocess.run(
        ["openssl", "pkcs12", "-export", "-nokeys", "-out", p12, "-password", "pass:changeit"],
        input=crl2pkcs7.stdout,
        capture_output=True,
        check=True,
    )

    result = subprocess.run(
        [
            keytool, "-importkeystore",
            "-srckeystore", p12, "-srcstoretype", "PKCS12", "-srcstorepass", "changeit",
            "-destkeystore", cacerts, "-deststorepass", "changeit",
            "-noprompt",
        ],
        capture_output=True,
        text=True,
    )

# keytool prints import counts to stderr
output = result.stderr + result.stdout
print(output.strip() if output.strip() else "Import complete")
if result.returncode != 0:
    sys.exit(result.returncode)
