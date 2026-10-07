#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

BRANCH="${GITHUB_BASE_REF:-}"
if [[ -z "$BRANCH" ]]; then
    if [[ -n "${GITHUB_REF:-}" ]]; then
        BRANCH="${GITHUB_REF#refs/heads/}"
    else
        BRANCH="$(git -C "$BASE_DIR" rev-parse --abbrev-ref HEAD)"
    fi
fi

if [[ "$BRANCH" != "master" ]]; then
    echo "check_community_license: skipped on '$BRANCH'"
    exit 0
fi

FAILED=0

fail() {
    echo "ERROR: $1" >&2
    FAILED=1
}

[[ -e "$BASE_DIR/LICENSE-APACHE" ]] && fail "LICENSE-APACHE exists on master. It belongs on the ee branch only."

for f in LICENSE README.md; do
    if grep -qiE "early access branch|Long Term Support branch" "$BASE_DIR/$f"; then
        fail "$f contains ee/LTS branch licence wording. Restore master's copy."
    fi
done

grep -q "Apache License" "$BASE_DIR/LICENSE" || fail "LICENSE does not contain the Apache 2 licence text."

if [[ "$FAILED" -ne 0 ]]; then
    echo "Licence check failed. Run: git checkout <last good master commit> -- LICENSE README.md && git rm -f LICENSE-APACHE" >&2
    exit 1
fi

echo "check_community_license: OK"
