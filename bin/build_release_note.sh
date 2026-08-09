#!/bin/bash

set -e

BASE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd)"

# Standing notice prepended to every release body. Renovate and Dependabot embed
# release bodies into the PRs they raise in consuming repos, so this is how the
# distribution change reaches users inside their own workflow.
# To remove: delete bin/release_notice.md (this block is skipped if absent).
if [ -f "$BASE_DIR/bin/release_notice.md" ]; then
  cat "$BASE_DIR/bin/release_notice.md"
  echo ""
fi

echo "Changelog:"
TAG=$(echo "refs/tags/$1" | sed "s/.*tags\///g")
START="### v$TAG"
END="###"
sed -n "/^$START$/,/$END/p" $BASE_DIR/CHANGELOG.md | sed '1d' | sed '$d' | sed '$d'
