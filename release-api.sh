#!/usr/bin/env bash

#if [[ `git rev-parse --abbrev-ref HEAD` != "master" ]]; then
#    echo "not master branch, so skipping"
#    exit 0
#fi

set -e
set -o errexit
set -o pipefail
set -o nounset

#TRAVIS_PULL_REQUEST=${TRAVIS_PULL_REQUEST:"false"}
#
#if [[ "$TRAVIS_PULL_REQUEST" != "false" ]]; then
#    echo "not master branch, so skipping"
#    exit 0
#fi

TMP=/tmp/http4k-api/
./gradlew clean dokka
rm -rf ${TMP}
git clone https://${GH_TOKEN}@github.com/http4k/api.git ${TMP}
rm -rf ${TMP}/*
cp -R build/ddoc/http4k/* ${TMP}/

cd ${TMP}
git add .
if [[ -z $(git status -s) ]]
then
    echo "tree is clean"
else
    git commit -am "release API docs"
    git push --force
fi

cd -
