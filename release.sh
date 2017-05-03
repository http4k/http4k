#!/bin/bash

set -e

NEW_VERSION=`./tools/jq -r .reekwest.new version.json`

echo Releasing and publishing v$NEW_VERSION

function upgrade {
    echo Upgrade $1 to $2
    sed -i '' s/$1/$2/g README.md
}

upgrade `./tools/jq -r .reekwest.old version.json` $NEW_VERSION

./gradlew -PreleaseVersion=$NEW_VERSION clean build \
    :reekwest:bintrayUpload \
    :reekwest-client-apache:bintrayUpload \
    :reekwest-server-jetty:bintrayUpload \
    :reekwest-server-netty:bintrayUpload \
    :reekwest-contract:bintrayUpload \
    :reekwest-templates-handlebars:bintrayUpload \
    :reekwest-formats-argo:bintrayUpload \
    :reekwest-formats-jackson:bintrayUpload

echo Remember to commit the updated README.md file!!
