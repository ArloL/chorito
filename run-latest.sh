#!/bin/sh

set -o errexit
set -o nounset
#set -o xtrace

OS="$(uname)"
if [ "${OS}" = "Linux" ]; then
  	platform=linux
elif [ "${OS}" = "Darwin" ]; then
  	platform=macos
else
	platform=windows
fi

cleanup() {
    currentExitCode=$?
    rm -f "./chorito"
    exit ${currentExitCode}
}

trap cleanup INT TERM EXIT

wget --quiet \
    --output-document="./chorito" \
    "https://github.com/ArloL/chorito/releases/latest/download/chorito-${platform}"

chmod +x "./chorito"

"./chorito" --version

"./chorito" "$@"
