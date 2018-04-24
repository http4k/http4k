#!/usr/bin/env bash

#if [[ `git rev-parse --abbrev-ref HEAD` != "master" ]]; then
#    echo "not master branch, so skipping"
#    exit 0
#fi

set -e
set -o errexit
set -o pipefail
set -o nounset


git add .
if [[ -z $(git status -s) ]]
then
    echo "tree is clean"
else
    git commit -am "release API docs"
    git push --force
fi

cd -
