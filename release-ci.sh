#!/bin/bash

echo `git rev-parse --abbrev-ref HEAD`

#if [[ `git rev-parse --abbrev-ref HEAD` != "master" ]]; then
#    echo "not master branch, so skipping"
#    exit 0
#fi

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=`jq -r .http4k.version $DIR/version.json`

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/http4k/maven/http4k-core/versions/_latest | jq -r .name`

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed"
    exit 0
fi

echo "Attempting to release $LOCAL_VERSION (old version $BINTRAY_VERSION)"

./gradlew -PreleaseVersion=$LOCAL_VERSION clean javadocJar assemble \
    :http4k-core:bintrayUpload \
    :http4k-aws:bintrayUpload \
    :http4k-cloudnative:bintrayUpload \
    :http4k-client-apache:bintrayUpload \
    :http4k-client-apache-async:bintrayUpload \
    :http4k-client-okhttp:bintrayUpload \
    :http4k-client-jetty:bintrayUpload \
    :http4k-client-websocket:bintrayUpload \
    :http4k-contract:bintrayUpload \
    :http4k-incubator:bintrayUpload \
    :http4k-format-argo:bintrayUpload \
    :http4k-format-gson:bintrayUpload \
    :http4k-format-jackson:bintrayUpload \
    :http4k-format-jackson-xml:bintrayUpload \
    :http4k-format-kotlinx-serialisation:bintrayUpload \
    :http4k-format-moshi:bintrayUpload \
    :http4k-format-xml:bintrayUpload \
    :http4k-jsonrpc:bintrayUpload \
    :http4k-metrics-micrometer:bintrayUpload \
    :http4k-multipart:bintrayUpload \
    :http4k-resilience4j:bintrayUpload \
    :http4k-security-oauth:bintrayUpload \
    :http4k-server-apache:bintrayUpload \
    :http4k-server-jetty:bintrayUpload \
    :http4k-server-ktorcio:bintrayUpload \
    :http4k-server-netty:bintrayUpload \
    :http4k-server-undertow:bintrayUpload \
    :http4k-serverless-lambda:bintrayUpload \
    :http4k-template-dust:bintrayUpload \
    :http4k-template-freemarker:bintrayUpload \
    :http4k-template-jade4j:bintrayUpload \
    :http4k-template-handlebars:bintrayUpload \
    :http4k-template-pebble:bintrayUpload \
    :http4k-template-thymeleaf:bintrayUpload \
    :http4k-testing-approval:bintrayUpload \
    :http4k-testing-chaos:bintrayUpload \
    :http4k-testing-hamkrest:bintrayUpload \
    :http4k-testing-servirtium:bintrayUpload \
    :http4k-testing-webdriver:bintrayUpload

function notify_slack {
    local MESSAGE=$1
    echo "Notifying on Slack..."
    curl -X POST -H 'Content-type: application/json' --data "{'text':'$MESSAGE'}" $SLACK_WEBHOOK
}

function notify_gitter {
    local MESSAGE=$1
    echo "Notifying on Gitter..."
    curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $GITTER_BEARER_TOKEN" "$GITTER_WEBHOOK"  -d "{'text':'$MESSAGE'}"
}

if [ $? -ne 0 ]; then
    notify_slack "Release has failed. Check <https://travis-ci.org/http4k/http4k-core/builds/$TRAVIS_BUILD_ID|Build #$TRAVIS_BUILD_NUMBER> for details."
else
    notify_slack "Released version <https://bintray.com/http4k/maven/http4k-core/$LOCAL_VERSION|$LOCAL_VERSION> (See <https://github.com/http4k/http4k/blob/master/CHANGELOG.md#changelog|Changelog> for details)."
    notify_gitter "Released version <https://bintray.com/http4k/maven/http4k-core/$LOCAL_VERSION|$LOCAL_VERSION> (See <https://github.com/http4k/http4k/blob/master/CHANGELOG.md#changelog|Changelog> for details)."
fi
