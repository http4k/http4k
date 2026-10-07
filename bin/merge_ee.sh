#!/usr/bin/env bash

# Merges the ee branch into master for a Community release, keeping master's own licence files.
#
# Usage: bin/merge_ee.sh [ref]     (default: origin/ee)

set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"
cd "$BASE_DIR"

EE_REF="${1:-origin/ee}"

# Files whose master version must always win. Files absent on master are removed.
BRANCH_SPECIFIC_FILES=(LICENSE LICENSE-APACHE README.md .gitattributes)

die() {
    echo "ERROR: $1" >&2
    exit 1
}

[[ "$(git rev-parse --abbrev-ref HEAD)" == "master" ]] || die "must be run on master"
[[ -z "$(git status --porcelain --untracked-files=no)" ]] || die "working tree has uncommitted changes"

git fetch origin

[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/master)" ]] || die "local master is not at origin/master - pull or push first"
git rev-parse --verify --quiet "$EE_REF" > /dev/null || die "unknown ref: $EE_REF"

if [[ -z "$(git log --oneline HEAD.."$EE_REF")" ]]; then
    echo "Nothing to merge: master already contains $EE_REF"
    exit 0
fi

echo "Merging $EE_REF into master (no commit)..."
MERGE_OK=1
git merge --no-ff --no-commit "$EE_REF" || MERGE_OK=0

echo "Restoring master's branch-specific files..."
for f in "${BRANCH_SPECIFIC_FILES[@]}"; do
    if git cat-file -e "HEAD:$f" 2> /dev/null; then
        git checkout HEAD -- "$f"
    else
        git rm -f --quiet --ignore-unmatch -- "$f"
        rm -f -- "$f"
    fi
done

"$BASE_DIR/bin/check_community_license.sh" || {
    echo "Licence check failed. Fix the files above, or abandon with: git merge --abort" >&2
    exit 1
}

if [[ -n "$(git diff --name-only --diff-filter=U)" ]]; then
    echo
    echo "Merge has conflicts in:"
    git diff --name-only --diff-filter=U
    echo
    echo "Resolve them, run bin/check_community_license.sh, then: git commit"
    echo "Or abandon with: git merge --abort"
    exit 1
fi

[[ "$MERGE_OK" -eq 1 ]] || die "merge failed for a reason other than conflicts - inspect with git status"

git commit --no-edit -m "Merge $EE_REF into master for Community release"

echo
echo "Done. Merged $EE_REF into master with master's licence files kept."
echo "Next: check the result, then bump version.json, tag and push as normal."
