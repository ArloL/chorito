#!/bin/sh

set -o errexit
set -o nounset
#set -o xtrace

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

cleanup() {
	currentExitCode=$?
	rm -f "./chorito"
	exit ${currentExitCode}
}

trap cleanup INT TERM EXIT

# The archive holds exactly one file, already named chorito and executable.
wget --quiet --output-document=- \
	"https://github.com/ArloL/chorito/releases/latest/download/${asset}" \
	| tar --extract --gzip

"./chorito" --version

"./chorito" "$@"
