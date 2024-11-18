#!/bin/bash

set -e

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

echo "Changelog:"
TAG=$(echo "refs/tags/$1" | sed "s/.*tags\///g")
START="### v$TAG"
END="###"
sed -n "/^$START$/,/$END/p" $BASE_DIR/CHANGELOG.md | sed '1d' | sed '$d' | sed '$d'
