#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

rm -Rf build/

./tools/embed_code.py

cp -R src/mkdocs.yml build/docs-website

cd build/docs-website

mkdocs build

aws s3 rm s3://http4k-new-design/ --recursive --profile http4k-playground
aws s3 cp site/ s3://http4k-new-design/ --recursive --profile http4k-playground
