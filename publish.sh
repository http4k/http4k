#!/bin/bash
echo Releasing and publishing v$1

./gradlew -PreleaseVersion=$1 clean build bintrayUpload