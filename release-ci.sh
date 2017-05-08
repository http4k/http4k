#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=`jq -r .http4k.version $DIR/version.json`

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/http4k-core/maven/http4k-core/versions/_latest | jq -r .name`

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed"
    exit 0
fi

echo "Attempting to release $LOCAL_VERSION (old version $BINTRAY_VERSION)"

./gradlew -PreleaseVersion=$LOCAL_VERSION clean build \
    :http4k-core:bintrayUpload \
    :http4k-client-apache:bintrayUpload \
    :http4k-server-jetty:bintrayUpload \
    :http4k-server-netty:bintrayUpload \
    :http4k-contract:bintrayUpload \
    :http4k-template-handlebars:bintrayUpload \
    :http4k-format-argo:bintrayUpload \
    :http4k-format-jackson:bintrayUpload

function notify_slack {
    local MESSAGE=$1
    echo "Notifying on Slack..."
    curl -X POST -H 'Content-type: application/json' --data "{'text':'$MESSAGE'}" $SLACK_WEBHOOK
}

if [ $? -ne 0 ]; then
    notify_slack "Release has failed. Check <https://travis-ci.org/http4k/http4k/builds/$TRAVIS_BUILD_ID|Build #$TRAVIS_BUILD_NUMBER> for details."
else
    notify_slack "Released version <https://bintray.com/http4k/maven/http4k/$LOCAL_VERSION|$LOCAL_VERSION>."
fi
