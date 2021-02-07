#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

source ./release-functions.sh

for i in $(./listProjects.sh); do
    maven_publish "$i"
done
