#!/bin/sh

set -o errexit
set -o nounset
#set -o xtrace

# The release asset names the os and the arch, so an Intel Mac cannot silently
# take an Apple silicon build. GraalVM cannot cross-compile and GitHub no longer
# runs Intel macOS runners, so that build is the only macOS one there is.
case "$(uname -s)/$(uname -m)" in
Linux/x86_64) asset=chorito-linux-x64.tar.gz ;;
Linux/aarch64 | Linux/arm64) asset=chorito-linux-arm64.tar.gz ;;
Darwin/arm64) asset=chorito-macos-arm64.tar.gz ;;
*)
	echo "No chorito build for $(uname -s) $(uname -m)." >&2
	echo "See https://github.com/ArloL/chorito/releases/latest" >&2
	exit 1
	;;
esac

mkdir -p "${HOME}/bin"

# The archive holds exactly one file, already named chorito and executable.
wget --quiet --output-document=- \
	"https://github.com/ArloL/chorito/releases/latest/download/${asset}" \
	| tar --extract --gzip --directory "${HOME}/bin"
