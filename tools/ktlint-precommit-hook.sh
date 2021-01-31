#!/bin/sh
# This is a ktlint pre-commit/pre-push hook to validate the introduced changes to conform to kotlin code style.
# To install this hook, copy this file to .git/hooks with the filename `pre-commit` or `pre-push` as required.
# If you want multiple scripts to run on a single commit hook refer to https://stackoverflow.com/questions/26624368/handle-multiple-pre-commit-hooks
# (originally sourced from: https://github.com/pinterest/ktlint/blob/master/ktlint/src/main/resources/ktlint-git-pre-commit-hook.sh)
git diff --name-only HEAD | grep '\.kt[s"]\?$' | xargs ktlint
