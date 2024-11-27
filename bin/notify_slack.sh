#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

MESSAGE="Released http4k Community version $1 (See <https://www.http4k.org/ecosystem/changelog/|Changelog> for details)."
curl -X POST -H 'Content-type: application/json' --data "{'text':'$MESSAGE'}" $SLACK_WEBHOOK
