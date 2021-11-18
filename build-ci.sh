#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

./gradlew check jacocoRootReport --build-cache -i
bash <(curl -s https://codecov.io/bash)
