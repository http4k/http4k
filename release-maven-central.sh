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
    echo "Publishing $PACKAGE..."
    RESULT=$(curl -s -X POST -u "$BINTRAY_USER:$BINTRAY_KEY" -H "Content-Type: application/json" --data "$PAYLOAD" "https://bintray.com/api/v1/maven_central_sync/http4k/maven/$PACKAGE/versions/$LOCAL_VERSION")

    if [[ ! "${RESULT}" =~ .*Successful.* ]]; then
       echo "Failed: ${RESULT}"
       exit 1
    fi
}

echo "Making $LOCAL_VERSION available in Maven central..."

#maven_publish "http4k-aws"
maven_publish "http4k-core"
maven_publish "http4k-contract"
maven_publish "http4k-template-handlebars"
maven_publish "http4k-template-pebble"
maven_publish "http4k-template-thymeleaf"
maven_publish "http4k-client-apache"
maven_publish "http4k-client-okhttp"
maven_publish "http4k-format-argo"
maven_publish "http4k-format-gson"
maven_publish "http4k-format-jackson"
maven_publish "http4k-server-jetty"
maven_publish "http4k-server-netty"
maven_publish "http4k-server-undertow"