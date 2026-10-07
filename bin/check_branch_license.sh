#!/usr/bin/env bash

# Checks that the licence files match the branch they are on.
#
# - master:          must NOT carry the commercial branch files (LICENSE-APACHE, ee/lts wording).
# - ee, next, lts:   MUST carry them, with the Http4kEE licence default.
# - other branches:  no-op.
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
LTS_MARKER="Long Term Support branch"
DEFAULT_LICENSE="$BASE_DIR/gradle/gradle-plugins/src/main/kotlin/org/http4k/default-license.gradle.kts"

expect_default_license() {
    grep -qE "defaultBranchLicense = ModuleLicense\.$1\b" "$DEFAULT_LICENSE" ||
        fail "default-license.gradle.kts on $BRANCH must set defaultBranchLicense = ModuleLicense.$1"
}
FAILED=0

fail() {
    echo "ERROR: $1" >&2
    FAILED=1
}

# A commercial branch carries LICENSE-APACHE, its own LICENSE/README wording and the Http4kEE default.
expect_commercial_branch() {
    local MARKER="$1"
    [[ -e "$BASE_DIR/LICENSE-APACHE" ]] || fail "LICENSE-APACHE is missing on $BRANCH."
    [[ -e "$BASE_DIR/LICENSE-APACHE" ]] && { grep -q "Apache License" "$BASE_DIR/LICENSE-APACHE" || fail "LICENSE-APACHE does not contain the Apache 2 licence text."; }
    for f in LICENSE README.md; do
        grep -qi "$MARKER" "$BASE_DIR/$f" || fail "$f on $BRANCH is missing the '$MARKER' wording."
    done
    expect_default_license Http4kEE
    RESTORE="git checkout <last good $BRANCH commit> -- LICENSE LICENSE-APACHE README.md version.json gradle/gradle-plugins/src/main/kotlin/org/http4k/default-license.gradle.kts"
}

case "$BRANCH" in
    master)
        [[ -e "$BASE_DIR/LICENSE-APACHE" ]] && fail "LICENSE-APACHE exists on master. It belongs on the commercial branches only."
        for f in LICENSE README.md; do
            grep -qiE "$EE_MARKER|$LTS_MARKER" "$BASE_DIR/$f" && fail "$f on master contains ee/lts branch wording."
        done
        grep -q "Apache License" "$BASE_DIR/LICENSE" || fail "LICENSE on master does not contain the Apache 2 licence text."
        expect_default_license Apache2
        RESTORE="git checkout <last good master commit> -- LICENSE README.md version.json gradle/gradle-plugins/src/main/kotlin/org/http4k/default-license.gradle.kts && git rm -f LICENSE-APACHE"
        ;;
    ee | next)
        # next carries v7 work and is rebased on ee, so it carries ee's licence files
        expect_commercial_branch "$EE_MARKER"
        ;;
    lts)
        expect_commercial_branch "$LTS_MARKER"
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
