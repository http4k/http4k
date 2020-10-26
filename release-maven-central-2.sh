#!/bin/bash

source ./release-functions.sh

maven_publish "http4k-client-apache"
maven_publish "http4k-client-apache4"
maven_publish "http4k-client-apache-async"
maven_publish "http4k-client-apache4-async"
maven_publish "http4k-client-jetty"
maven_publish "http4k-client-okhttp"
maven_publish "http4k-client-websocket"

maven_publish "http4k-serverless-alibaba"
maven_publish "http4k-serverless-gcf"
maven_publish "http4k-serverless-lambda"
maven_publish "http4k-serverless-openwhisk"
maven_publish "http4k-serverless-tencent"
