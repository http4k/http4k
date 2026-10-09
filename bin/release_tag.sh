#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

LOCAL_VERSION=$(jq -r .http4k.version $BASE_DIR/version.json)

function create_tag {
    git tag -a "$LOCAL_VERSION" -m "http4k version $LOCAL_VERSION"
    git push origin "$LOCAL_VERSION"
}

function ensure_version_matches_branch {
    local BRANCH="${GITHUB_REF_NAME:-$(git rev-parse --abbrev-ref HEAD)}"

    case "$BRANCH" in
        ee)
            [[ "$LOCAL_VERSION" == *-ee ]] || { echo "ERROR: version $LOCAL_VERSION on ee must end in -ee" >&2; exit 1; } ;;
        lts)
            [[ "$LOCAL_VERSION" == *-lts ]] || { echo "ERROR: version $LOCAL_VERSION on lts must end in -lts" >&2; exit 1; } ;;
        *)
            [[ "$LOCAL_VERSION" != *-ee && "$LOCAL_VERSION" != *-lts ]] || { echo "ERROR: version $LOCAL_VERSION has an ee/lts suffix on $BRANCH" >&2; exit 1; } ;;
    esac
}

function ensure_release_commit {
    local CHANGED_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD)

    if [[ "$CHANGED_FILES" != *version.json* ]]; then
        echo "Version did not change on this commit. Ignoring"; exit 0;
    fi
}

cd $BASE_DIR

ensure_release_commit

ensure_version_matches_branch

create_tag

