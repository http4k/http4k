#!/bin/bash

source ./release-functions.sh

ensure_release_commit

maven_publish "http4k-core"
maven_publish "http4k-server-apache"
maven_publish "http4k-server-jetty"
maven_publish "http4k-server-netty"
maven_publish "http4k-server-undertow"
maven_publish "http4k-serverless-lambda"
maven_publish "http4k-contract"
