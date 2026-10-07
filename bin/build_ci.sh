#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

"$BASE_DIR"/bin/check_community_license.sh

"$BASE_DIR"/gradlew check checkLicense jacocoRootReport --build-cache
