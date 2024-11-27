#!/bin/bash

VERSION=$1

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

$BASE_DIR/gradlew listProjects -q 2> $BASE_DIR/projects.txt

for ARTIFACT in `cat $BASE_DIR/projects.txt`
do
    curl -X PURGE https://repo.maven.apache.org/maven2/org/http4k/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.pom > /dev/null 2>&1

    echo Purging $ARTIFACT $VERSION `curl -sq --head https://repo.maven.apache.org/maven2/org/http4k/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.pom | head -1`
done

rm $BASE_DIR/projects.txt
