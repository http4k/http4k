#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

NEW_VERSION=$1

BINTRAY_VERSION=$(curl -s https://bintray.com/api/v1/packages/http4k/maven/http4k-core/versions/_latest | tools/jq -r .name)

git stash

echo Upgrade from "$BINTRAY_VERSION" to "$NEW_VERSION"

find . -name "*.md" | grep -v "CHANGELOG" | xargs -I '{}' sed -i '' s/"$BINTRAY_VERSION"/"$NEW_VERSION"/g '{}'
sed -i '' s/"$BINTRAY_VERSION"/"$NEW_VERSION"/g version.json

git commit -am"Release $NEW_VERSION"

git push

git stash apply
