#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

./gradlew dokkaHtmlMultiModule
rm -rf /tmp/http4k-api/
git clone https://"${GH_TOKEN}"@github.com/http4k/api.git /tmp/http4k-api/
rm -rf /tmp/http4k-api/*
cp -R build/ddoc/http4k/* /tmp/http4k-api/

cd /tmp/http4k-api
git add .
if [[ -z $(git status -s) ]]
then
    echo "tree is clean"
else
    git commit -am "release API docs"
    git push --force
fi

cd -
