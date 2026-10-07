#!/usr/bin/env bash

# Checks that the licence files match the branch they are on.
#
# - master: must NOT carry the ee licence files (LICENSE-APACHE, ee LICENSE/README wording).
# - ee:     MUST carry them.
# - other branches: no-op.
#
# Catches a merge between master and ee that skipped bin/merge_ee.sh or bin/merge_master.sh,
# for example one done through the GitHub UI.

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

EE_MARKER="early access branch"
FAILED=0

fail() {
    echo "ERROR: $1" >&2
    FAILED=1
}

case "$BRANCH" in
    master)
        [[ -e "$BASE_DIR/LICENSE-APACHE" ]] && fail "LICENSE-APACHE exists on master. It belongs on ee only."
        for f in LICENSE README.md; do
            grep -qi "$EE_MARKER" "$BASE_DIR/$f" && fail "$f on master contains ee branch wording."
        done
        grep -q "Apache License" "$BASE_DIR/LICENSE" || fail "LICENSE on master does not contain the Apache 2 licence text."
        RESTORE="git checkout <last good master commit> -- LICENSE README.md && git rm -f LICENSE-APACHE"
        ;;
    ee)
        [[ -e "$BASE_DIR/LICENSE-APACHE" ]] || fail "LICENSE-APACHE is missing on ee."
        [[ -e "$BASE_DIR/LICENSE-APACHE" ]] && { grep -q "Apache License" "$BASE_DIR/LICENSE-APACHE" || fail "LICENSE-APACHE does not contain the Apache 2 licence text."; }
        for f in LICENSE README.md; do
            grep -qi "$EE_MARKER" "$BASE_DIR/$f" || fail "$f on ee is missing the ee branch wording."
        done
        RESTORE="git checkout <last good ee commit> -- LICENSE LICENSE-APACHE README.md"
        ;;
    *)
        echo "check_branch_license: skipped on '$BRANCH'"
        exit 0
        ;;
esac

if [[ "$FAILED" -ne 0 ]]; then
    echo "Licence check failed on $BRANCH. To restore: $RESTORE" >&2
    exit 1
fi

echo "check_branch_license: OK ($BRANCH)"
