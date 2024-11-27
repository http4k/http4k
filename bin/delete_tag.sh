#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

LOCAL_VERSION=$1

echo "Deleting tag $LOCAL_VERSION..."

cd $BASE_DIR

git tag -d "$LOCAL_VERSION"
git push --delete origin "$LOCAL_VERSION"
