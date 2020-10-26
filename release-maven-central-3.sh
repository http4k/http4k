#!/bin/bash

source ./release-functions.sh

maven_publish "http4k-format-jackson"
maven_publish "http4k-format-gson"
maven_publish "http4k-format-moshi"
maven_publish "http4k-format-argo"
maven_publish "http4k-format-jackson-xml"
maven_publish "http4k-format-jackson-yaml"
maven_publish "http4k-format-xml"
maven_publish "http4k-format-kotlinx-serialization"

maven_publish "http4k-jsonrpc"
maven_publish "http4k-multipart"
maven_publish "http4k-aws"
maven_publish "http4k-resilience4j"
