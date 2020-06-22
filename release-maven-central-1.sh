#!/bin/bash

source ./release-functions.sh

maven_publish "http4k-core"
maven_publish "http4k-contract"
maven_publish "http4k-security-oauth"
maven_publish "http4k-cloudnative"
maven_publish "http4k-metrics-micrometer"

maven_publish "http4k-server-undertow"
maven_publish "http4k-server-jetty"
maven_publish "http4k-server-apache"
maven_publish "http4k-server-apache4"
maven_publish "http4k-server-ktorcio"
maven_publish "http4k-server-ktornetty"
maven_publish "http4k-server-netty"
maven_publish "http4k-server-ratpack"

