#!/usr/bin/env python3
"""Install GraalVM CE into the mise Java installs directory."""

import os
import shutil
import sys
import tarfile
import tempfile
import urllib.request

TOOL_VERSIONS = os.path.join(os.environ["CLAUDE_PROJECT_DIR"], ".tool-versions")

# Parse: "java graalvm-community-25.0.2"
java_version = None
with open(TOOL_VERSIONS) as f:
    for line in f:
        parts = line.split()
        if len(parts) == 2 and parts[0] == "java":
            java_version = parts[1]
            break

if not java_version:
    print(f"ERROR: no java entry found in {TOOL_VERSIONS}", file=sys.stderr)
    sys.exit(1)

# graalvm-community-25.0.2 -> 25.0.2
PREFIX = "graalvm-community-"
if not java_version.startswith(PREFIX):
    print(f"ERROR: expected java version to start with {PREFIX!r}, got {java_version!r}", file=sys.stderr)
    sys.exit(1)

version_number = java_version[len(PREFIX):]
graalvm_tag = f"jdk-{version_number}"
graalvm_file = f"graalvm-community-jdk-{version_number}_linux-x64_bin.tar.gz"
graalvm_url = f"https://github.com/graalvm/graalvm-ce-builds/releases/download/{graalvm_tag}/{graalvm_file}"

install_dir = os.path.join(
    os.path.expanduser("~"),
    ".local", "share", "mise", "installs", "java", java_version,
)

if os.path.isdir(install_dir):
    print(f"GraalVM CE {java_version} already installed at {install_dir}")
    sys.exit(0)

print(f"Downloading GraalVM CE from {graalvm_url} ...")
with tempfile.TemporaryDirectory() as tmp:
    archive = os.path.join(tmp, graalvm_file)
    urllib.request.urlretrieve(graalvm_url, archive)

    print("Extracting...")
    with tarfile.open(archive, "r:gz") as tf:
        tf.extractall(tmp)

    # The extracted directory name includes a build suffix we don't know upfront,
    # e.g. graalvm-community-openjdk-25.0.2+10.1 — find it by listing tmp.
    candidates = [
        d for d in os.listdir(tmp)
        if os.path.isdir(os.path.join(tmp, d)) and d != archive
    ]
    if len(candidates) != 1:
        print(f"ERROR: expected one extracted dir in {tmp}, found: {candidates}", file=sys.stderr)
        sys.exit(1)

    os.makedirs(os.path.dirname(install_dir), exist_ok=True)
    shutil.move(os.path.join(tmp, candidates[0]), install_dir)

print(f"GraalVM CE {java_version} installed to {install_dir}")
