#!/bin/bash

set -e

TAG=$(echo "refs/tags/$1" | sed "s/.*tags\///g")
START="### v$TAG"
END="###"
sed -n "/^$START$/,/$END/p" CHANGELOG.md | sed '1d' | sed '$d' | sed '$d'
