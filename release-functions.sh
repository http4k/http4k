#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=$(jq -r .http4k.version $DIR/version.json)

function maven_publish {
    local PACKAGE=$1
    local PAYLOAD="{\"username\": \"${SONATYPE_USER}\", \"password\": \"${SONATYPE_KEY}\"}"

    local PUBLISHED=$(curl --fail --silent -o /dev/null https://repo.maven.apache.org/maven2/org/http4k/"${PACKAGE}"/"${LOCAL_VERSION}"/"${PACKAGE}"-"${LOCAL_VERSION}".pom ; echo $?)

    if [[ $PUBLISHED == "0" ]]; then
        echo "$PACKAGE is already published. Skipping"
    else
        echo "Publishing $PACKAGE $LOCAL_VERSION into Maven central..."
        RESULT=$(curl -s -X POST -u "$BINTRAY_USER:$BINTRAY_KEY" -H "Content-Type: application/json" --data "$PAYLOAD" "https://bintray.com/api/v1/maven_central_sync/http4k/maven/$PACKAGE/versions/$LOCAL_VERSION")

        if [[ ! "${RESULT}" =~ .*Successful.* ]]; then
            echo "Failed: ${RESULT}"
        fi
    fi
}

function create_tag {
    git tag -a "$LOCAL_VERSION" -m "http4k version $LOCAL_VERSION"
    git push origin "$LOCAL_VERSION"
}

function ensure_release_commit {
    local CHANGED_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD)

    if [[ "$CHANGED_FILES" != *version.json* ]]; then
        echo "Version did not change on this commit. Ignoring"; exit 0;
    fi
}

