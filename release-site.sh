#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

pip install -r requirements.txt

TMP=/tmp/http4k.github.io/
rm -rf ${TMP}
git clone https://${GH_TOKEN}@github.com/http4k/http4k.github.io.git ${TMP}
cp -R src/docs ${TMP}/

cp CONTRIBUTING.md ${TMP}docs/contributing/index.md
cp CHANGELOG.md ${TMP}docs/changelog/index.md
cp README.md ${TMP}docs/index.md
cp -R src/mkdocs.yml ${TMP}

cd ${TMP}
mkdocs gh-deploy

echo "hello1"
git reset --hard
echo "hello2"
git pull --rebase
echo "hello3"
sed s/\>\</\>`date '+%Y-%m-%d'`\</g sitemap.xml > new ; mv new sitemap.xml
echo "hello4"
git add sitemap.xml
echo "hello5"
git commit -m "sitemap update"
echo "hello6"
git push
echo "hello7"

cd -
