#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

DEBUG=true "$BASE_DIR"/gradlew :http4k-server-shutdown-integration-test:integrationTests --build-cache --info

