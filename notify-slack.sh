#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=`jq -r .http4k.version $DIR/version.json`

MESSAGE="Released version <https://bintray.com/http4k/maven/http4k-core/$LOCAL_VERSION|$LOCAL_VERSION> (See <https://github.com/http4k/http4k/blob/master/CHANGELOG.md#changelog|Changelog> for details)."
curl -X POST -H 'Content-type: application/json' --data "{'text':'$MESSAGE'}" $SLACK_WEBHOOK
