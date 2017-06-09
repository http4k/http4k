#!/usr/bin/env bash

pip install mkdocs
pip install mkdocs-material

TMP=/tmp/http4k.github.io/
rm -rf ${TMP}
git clone git@github.com:http4k/http4k.github.io.git ${TMP}
cp -R docs ${TMP}/
cp -R mkdocs.yml ${TMP}

cd ${TMP}
mkdocs gh-deploy --remote-branch master
cd -
