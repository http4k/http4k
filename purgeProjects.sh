#!/bin/bash

VERSION=$1

./gradlew listProjects -q 2> projects.txt

for ARTIFACT in `cat projects.txt`
do
   echo Purging $ARTIFACT $VERSION
   curl -X PURGE https://repo.maven.apache.org/maven2/org/http4k/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.pom > /dev/null 2>&1
   curl https://repo.maven.apache.org/maven2/org/http4k/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.pom > /dev/null 2>&1
done

rm projects.txt
