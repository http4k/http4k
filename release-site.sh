#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

pip install -r requirements.txt

./tools/embed_code.py

export TOKEN_TO_USE=${GH_TOKEN:$1}

TMP=/tmp/http4k.github.io/
rm -rf ${TMP}
git clone https://${TOKEN_TO_USE}@github.com/http4k/http4k.github.io.git ${TMP}
cp -R build/docs-website/docs ${TMP}/

cp -R src/mkdocs.yml ${TMP}

cd ${TMP}
mkdocs gh-deploy

git reset --hard
git pull --rebase
sed s/\>\</\>`date '+%Y-%m-%d'`\</g sitemap.xml > new ; mv new sitemap.xml
git add sitemap.xml

set +e
git commit -m "sitemap update"
git push

cd -
