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
       exit 1
    fi
}

echo "Deleting $LOCAL_VERSION..."

maven_publish "http4k-aws"
maven_publish "http4k-core"
maven_publish "http4k-contract"
maven_publish "http4k-template-dust"
maven_publish "http4k-template-freemarker"
maven_publish "http4k-template-handlebars"
maven_publish "http4k-template-pebble"
maven_publish "http4k-template-thymeleaf"
maven_publish "http4k-template-jade4j"
maven_publish "http4k-client-apache"
maven_publish "http4k-client-apache-async"
maven_publish "http4k-client-jetty"
maven_publish "http4k-client-okhttp"
maven_publish "http4k-client-websocket"
maven_publish "http4k-format-argo"
maven_publish "http4k-format-gson"
maven_publish "http4k-format-jackson"
maven_publish "http4k-format-jackson-xml"
#maven_publish "http4k-format-kotlinx-serialization"
maven_publish "http4k-format-moshi"
maven_publish "http4k-format-xml"
maven_publish "http4k-metrics-micrometer"
maven_publish "http4k-multipart"
maven_publish "http4k-resilience4j"
maven_publish "http4k-security-oauth"
maven_publish "http4k-server-apache"
maven_publish "http4k-server-jetty"
maven_publish "http4k-server-netty"
maven_publish "http4k-server-undertow"
maven_publish "http4k-serverless-lambda"
maven_publish "http4k-testing-hamkrest"
maven_publish "http4k-testing-webdriver"
