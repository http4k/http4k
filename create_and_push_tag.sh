#!/bin/bash
set -e

NEW_VERSION=$1

git tag -a "$NEW_VERSION" -m "http4k version $NEW_VERSION"
git push origin "$NEW_VERSION"
