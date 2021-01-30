#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

./gradlew -i check jacocoRootReport --build-cache
bash <(curl -s https://codecov.io/bash)
