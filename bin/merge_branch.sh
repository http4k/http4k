#!/usr/bin/env bash

# Merges one release branch into another while keeping the target branch's own copies of the
# branch-specific files (licence files, README, version, licence defaults).
#
# Supported directions:
#   on master: bin/merge_branch.sh ee       - quarterly Community release (or use bin/merge_ee.sh)
#   on ee:     bin/merge_branch.sh master   - routine sync (or use bin/merge_master.sh)
#
# A plain merge - or a merge through the GitHub UI - would carry the source branch's copies across.
# This script merges without committing, restores the target's copies, verifies the result with
# bin/check_branch_license.sh, then commits. It does not push.
#
# Usage: bin/merge_branch.sh <source-branch> [source-ref]     (default ref: origin/<source-branch>)

set -o errexit
set -o pipefail
set -o nounset

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"
cd "$BASE_DIR"

# Files whose target-branch version must always win. Files absent on the target are removed.
BRANCH_SPECIFIC_FILES=(
    LICENSE
    LICENSE-APACHE
    README.md
    version.json
    gradle/gradle-plugins/src/main/kotlin/org/http4k/default-license.gradle.kts
)

die() {
    echo "ERROR: $1" >&2
    exit 1
}

[[ $# -ge 1 ]] || die "usage: bin/merge_branch.sh <source-branch> [source-ref]"

SOURCE="$1"
SOURCE_REF="${2:-origin/$SOURCE}"
TARGET="$(git rev-parse --abbrev-ref HEAD)"

case "$TARGET:$SOURCE" in
    master:ee) NEXT="check the result, then bump version.json, tag and push as normal." ;;
    ee:master) NEXT="check the result, then push." ;;
    *) die "unsupported merge: $SOURCE into $TARGET (supported: ee into master, master into ee)" ;;
esac

[[ -z "$(git status --porcelain --untracked-files=no)" ]] || die "working tree has uncommitted changes"

git fetch origin

[[ "$(git rev-parse HEAD)" == "$(git rev-parse "origin/$TARGET")" ]] || die "local $TARGET is not at origin/$TARGET - pull or push first"
git rev-parse --verify --quiet "$SOURCE_REF" > /dev/null || die "unknown ref: $SOURCE_REF"

if [[ -z "$(git log --oneline HEAD.."$SOURCE_REF")" ]]; then
    echo "Nothing to merge: $TARGET already contains $SOURCE_REF"
    exit 0
fi

echo "Merging $SOURCE_REF into $TARGET (no commit)..."
git merge --no-ff --no-commit "$SOURCE_REF" || true
git rev-parse --quiet --verify MERGE_HEAD > /dev/null || die "merge did not start - inspect with git status"

echo "Restoring $TARGET's branch-specific files..."
for f in "${BRANCH_SPECIFIC_FILES[@]}"; do
    if git cat-file -e "HEAD:$f" 2> /dev/null; then
        git checkout HEAD -- "$f"
    else
        git rm -f --quiet --ignore-unmatch -- "$f"
        rm -f -- "$f"
    fi
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

git commit --no-edit -m "Merge $SOURCE_REF into $TARGET"

echo
echo "Done. Merged $SOURCE_REF into $TARGET with $TARGET's branch-specific files kept."
echo "Next: $NEXT"
