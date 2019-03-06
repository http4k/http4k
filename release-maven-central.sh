#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

./release-maven-central-1.sh
./release-maven-central-2.sh