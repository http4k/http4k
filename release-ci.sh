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

./gradlew --debug -PreleaseVersion=$LOCAL_VERSION clean build \
    :reekwest:bintrayUpload \
    :reekwest-client-apache:bintrayUpload \
    :reekwest-server-jetty:bintrayUpload \
    :reekwest-server-netty:bintrayUpload \
    :reekwest-contract:bintrayUpload \
    :reekwest-templates-handlebars:bintrayUpload \
    :reekwest-formats-argo:bintrayUpload \
    :reekwest-formats-jackson:bintrayUpload