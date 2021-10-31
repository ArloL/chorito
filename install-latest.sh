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

wget --quiet \
    --output-document="${HOME}/bin/chorito" \
    "https://github.com/ArloL/chorito/releases/latest/download/chorito-${platform}"

chmod +x "${HOME}/bin/chorito"
