#!/bin/bash
set -e

source ./release-functions.sh

git clone https://"${GH_TOKEN}"@github.com/http4k/http4k.git tmp/

pushd tmp
create_tag
popd
