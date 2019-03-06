#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=`jq -r .http4k.version $DIR/version.json`

function maven_publish {
    local PACKAGE=$1
    local PAYLOAD="{\"username\": \"${SONATYPE_USER}\", \"password\": \"${SONATYPE_KEY}\"}"

    local PUBLISHED=$(curl --fail --silent -o /dev/null https://mvnrepository.com/artifact/org.http4k/${PACKAGE}/${LOCAL_VERSION} ; echo $?)

    if [[ $PUBLISHED == "0" ]]; then
        echo "$PACKAGE is already published. Skipping"
    else
        echo "Publishing $PACKAGE..."
        RESULT=$(curl -s -X POST -u "$BINTRAY_USER:$BINTRAY_KEY" -H "Content-Type: application/json" --data "$PAYLOAD" "https://bintray.com/api/v1/maven_central_sync/http4k/maven/$PACKAGE/versions/$LOCAL_VERSION")

        if [[ ! "${RESULT}" =~ .*Successful.* ]]; then
           echo "Failed: ${RESULT}"
           exit 1
        fi
    fi
}

function ensure_release_commit {
    local CHANGED_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD)

    if [[ "$CHANGED_FILES" != *version.json* ]]; then
        echo "Version did not change on this commit. Ignoring"; exit 0;
    fi
}

ensure_release_commit

echo "Making $LOCAL_VERSION available in Maven central..."

maven_publish "http4k-format-argo"
maven_publish "http4k-format-moshi"
maven_publish "http4k-format-xml"
maven_publish "http4k-jsonrpc"
maven_publish "http4k-template-dust"
maven_publish "http4k-template-handlebars"
maven_publish "http4k-template-pebble"
maven_publish "http4k-template-thymeleaf"
maven_publish "http4k-resilience4j"
maven_publish "http4k-serverless-lambda"
maven_publish "http4k-security-oauth"
maven_publish "http4k-testing-chaos"
maven_publish "http4k-testing-hamkrest"
maven_publish "http4k-testing-webdriver"
maven_publish "http4k-metrics-micrometer"
