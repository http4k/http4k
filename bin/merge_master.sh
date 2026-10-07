#!/usr/bin/env bash

# Merges master into the ee branch, keeping ee's own licence files.
#
# ee carries a commercial-default LICENSE, LICENSE-APACHE and an ee README banner. A plain merge -
# or a merge through the GitHub UI - can replace them with master's versions. This script merges
# without committing, restores ee's copies, verifies the result, then commits.
#
# Usage: bin/merge_master.sh [ref]     (default: origin/master)
#
# Does not push.

set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"
cd "$BASE_DIR"

MASTER_REF="${1:-origin/master}"

# Files whose ee version must always win.
BRANCH_SPECIFIC_FILES=(LICENSE LICENSE-APACHE README.md)

die() {
    echo "ERROR: $1" >&2
    exit 1
}

[[ "$(git rev-parse --abbrev-ref HEAD)" == "ee" ]] || die "must be run on ee"
[[ -z "$(git status --porcelain --untracked-files=no)" ]] || die "working tree has uncommitted changes"

git fetch origin

[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/ee)" ]] || die "local ee is not at origin/ee - pull or push first"
git rev-parse --verify --quiet "$MASTER_REF" > /dev/null || die "unknown ref: $MASTER_REF"

if [[ -z "$(git log --oneline HEAD.."$MASTER_REF")" ]]; then
    echo "Nothing to merge: ee already contains $MASTER_REF"
    exit 0
fi

echo "Merging $MASTER_REF into ee (no commit)..."
git merge --no-ff --no-commit "$MASTER_REF" || true
git rev-parse --quiet --verify MERGE_HEAD > /dev/null || die "merge did not start - inspect with git status"

echo "Restoring ee's branch-specific files..."
for f in "${BRANCH_SPECIFIC_FILES[@]}"; do
    git checkout HEAD -- "$f"
done

"$BASE_DIR/bin/check_branch_license.sh" || {
    echo "Licence check failed. Fix the files above, or abandon with: git merge --abort" >&2
    exit 1
}

if [[ -n "$(git diff --name-only --diff-filter=U)" ]]; then
    echo
    echo "Merge has conflicts in:"
    git diff --name-only --diff-filter=U
    echo
    echo "Resolve them, run bin/check_branch_license.sh, then: git commit"
    echo "Or abandon with: git merge --abort"
    exit 1
fi


git commit --no-edit -m "Merge $MASTER_REF into ee"

echo
echo "Done. Merged $MASTER_REF into ee with ee's licence files kept."
echo "Next: check the result, then push."
