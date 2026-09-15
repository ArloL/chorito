# chorito

A tool that does some chores in your source code.

## Install

```bash
mise use github:ArloL/chorito
brew install arlol/tap/chorito
```

Or take the archive for your platform from the
[latest release](https://github.com/ArloL/chorito/releases/latest) — each one
holds a single `chorito` binary. Apple silicon is the only macOS build: GraalVM
cannot cross-compile and GitHub no longer runs Intel macOS runners.

To install the latest version to `~/bin/` instead:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/ArloL/chorito/HEAD/install-latest.sh)"
```

## Quickstart

To run the latest version without installing it:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/ArloL/chorito/HEAD/run-latest.sh)"
```
