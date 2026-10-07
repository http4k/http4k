#!/usr/bin/env bash

# Merges master into ee. Run on ee. See bin/merge_branch.sh.
#
# Usage: bin/merge_master.sh [ref]     (default: origin/master)

exec "$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)/merge_branch.sh" master "$@"
