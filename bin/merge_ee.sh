#!/usr/bin/env bash

# Merges ee into master for a quarterly Community release. Run on master. See bin/merge_branch.sh.
#
# Usage: bin/merge_ee.sh [ref]     (default: origin/ee)

exec "$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)/merge_branch.sh" ee "$@"
