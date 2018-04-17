#!/usr/bin/env bash

#if [[ `git rev-parse --abbrev-ref HEAD` != "master" ]]; then
#    echo "not master branch, so skipping"
#    exit 0
#fi

set -e
set -o errexit
set -o pipefail
set -o nounset

TRAVIS_PULL_REQUEST=${TRAVIS_PULL_REQUEST:false}

if [[ "$TRAVIS_PULL_REQUEST" != "false" ]]; then
    echo "not master branch, so skipping"
    exit 0
fi

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

git reset --hard
git pull --rebase
sed s/\>\</\>`date '+%Y-%m-%d'`\</g sitemap.xml > new ; mv new sitemap.xml
git add sitemap.xml
git commit -m "sitemap update"
git push

cd -
