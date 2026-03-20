#!/usr/bin/env python3
"""Install GraalVM CE into the mise Java installs directory."""

import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request

JAVA_VERSION = "graalvm-community-25.0.2"
GRAALVM_TAG = "jdk-25.0.2"
GRAALVM_FILE = "graalvm-community-jdk-25.0.2_linux-x64_bin.tar.gz"
GRAALVM_URL = f"https://github.com/graalvm/graalvm-ce-builds/releases/download/{GRAALVM_TAG}/{GRAALVM_FILE}"
EXTRACTED_DIR_NAME = "graalvm-community-openjdk-25.0.2+10.1"

install_dir = os.path.join(
    os.path.expanduser("~"),
    ".local", "share", "mise", "installs", "java", JAVA_VERSION,
)

if os.path.isdir(install_dir):
    print(f"GraalVM CE {JAVA_VERSION} already installed at {install_dir}")
    sys.exit(0)

print(f"Downloading GraalVM CE from {GRAALVM_URL} ...")
with tempfile.TemporaryDirectory() as tmp:
    archive = os.path.join(tmp, GRAALVM_FILE)
    urllib.request.urlretrieve(GRAALVM_URL, archive)

    print("Extracting...")
    with tarfile.open(archive, "r:gz") as tf:
        tf.extractall(tmp)

    extracted = os.path.join(tmp, EXTRACTED_DIR_NAME)
    if not os.path.isdir(extracted):
        print(f"ERROR: expected extracted dir {extracted!r} not found", file=sys.stderr)
        sys.exit(1)

    os.makedirs(os.path.dirname(install_dir), exist_ok=True)
    shutil.move(extracted, install_dir)

print(f"GraalVM CE {JAVA_VERSION} installed to {install_dir}")
