#!/bin/bash

set -e

NEW_VERSION=$1

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/reekwest/maven/reekwest/versions/_latest | tools/jq -r .name`

git stash

echo Upgrade from $BINTRAY_VERSION to $NEW_VERSION

sed -i '' s/$BINTRAY_VERSION/$NEW_VERSION/g README.md
sed -i '' s/$BINTRAY_VERSION/$NEW_VERSION/g version.json

git commit -am"Release $NEW_VERSION"

git stash apply
