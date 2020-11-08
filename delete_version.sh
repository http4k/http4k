#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=$1

function maven_publish {
    local PACKAGE=$1
    echo "Deleting $PACKAGE..."
    RESULT=$(curl -s -X DELETE -u "$BINTRAY_USER:$BINTRAY_KEY"  "https://api.bintray.com/packages/http4k/maven/$PACKAGE/versions/$LOCAL_VERSION")

    if [[ ! "${RESULT}" =~ .*success.* ]]; then
       echo "Failed: ${RESULT}"
#       exit 1
    fi
}

echo "Deleting $LOCAL_VERSION..."

for i in $(./listProjects.sh); do
    maven_publish "$i"
done
