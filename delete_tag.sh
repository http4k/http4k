#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

LOCAL_VERSION=$1

echo "Deleting tag $LOCAL_VERSION..."

git tag -d "$LOCAL_VERSION"
git push --delete origin "$LOCAL_VERSION"
