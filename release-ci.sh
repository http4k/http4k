#!/bin/bash

set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=`jq -r .reekwest.new $DIR/version.json`

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/reekwest/maven/reekwest/versions/_latest | jq -r .name`

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed"
    exit 0
fi
