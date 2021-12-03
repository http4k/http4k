#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

NEW_VERSION=$1

LATEST_VERSION=$(aws --profile http4k-release s3 cp s3://http4k/latest-broadcasted-version.txt -)

git stash

echo Upgrade from "$LATEST_VERSION" to "$NEW_VERSION"

find . -name "*.md" | grep -v "CHANGELOG" | xargs -I '{}' sed -i '' s/"$LATEST_VERSION"/"$NEW_VERSION"/g '{}'
sed -i '' s/"$LATEST_VERSION"/"$NEW_VERSION"/g version.json

git commit -am"Release $NEW_VERSION"

git push

git stash apply
