#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

"$BASE_DIR"/gradlew check jacocoRootReport --build-cache --no-parallel --info
bash <(curl -s https://codecov.io/bash)
