#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=`jq -r .reekwest.version $DIR/version.json`

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/reekwest/maven/reekwest/versions/_latest | jq -r .name`

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed"
    exit 0
fi

echo "Attempting to release $LOCAL_VERSION (old version $BINTRAY_VERSION)"

./gradlew -PreleaseVersion=$LOCAL_VERSION clean build \
    :reekwest:bintrayUpload \
    :reekwest-client-apache:bintrayUpload \
    :reekwest-server-jetty:bintrayUpload \
    :reekwest-server-netty:bintrayUpload \
    :reekwest-contract:bintrayUpload \
    :reekwest-templates-handlebars:bintrayUpload \
    :reekwest-formats-argo:bintrayUpload \
    :reekwest-formats-jackson:bintrayUpload

function notify_slack {
    local MESSAGE=$1
    echo "Notifying on Slack..."
    curl -X POST -H 'Content-type: application/json' --data "{'text':'$MESSAGE'}" $SLACK_WEBHOOK
}

if [ $? -ne 0 ]; then
    notify_slack "Release has failed. Check <https://travis-ci.org/reekwest/reekwest/builds/$TRAVIS_BUILD_ID|Build #$TRAVIS_BUILD_NUMBER> for details."
else
    notify_slack "Released version <https://bintray.com/reekwest/maven/reekwest/$LOCAL_VERSION|$LOCAL_VERSION>."
fi
