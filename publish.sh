#!/bin/bash
echo Releasing and publishing v$1

./gradlew -PreleaseVersion=$1 clean build :reekwest:bintrayUpload :reekwest-client-apache:bintrayUpload :reekwest-server-jetty:bintrayUpload :reekwest-contract:bintrayUpload