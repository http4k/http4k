#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

./gradlew -i check jacocoRootReport
bash <(curl -s https://codecov.io/bash)

LOCAL_VERSION=$(jq -r .http4k.version version.json)

BINTRAY_VERSION=$(curl -s https://bintray.com/api/v1/packages/http4k/maven/http4k-core/versions/_latest | jq -r .name)

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed: $BINTRAY_VERSION"
    exit 0
fi

echo "Attempting to release $LOCAL_VERSION (old version $BINTRAY_VERSION)"

