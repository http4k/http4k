#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

MESSAGE="Released http4k version $1 (See <https://www.http4k.org/ecosystem/http4k/changelog/|Changelog> for details)."
curl -X POST -H 'Content-type: application/json' --data "{'text':'$MESSAGE'}" $SLACK_WEBHOOK

if [[ $1 == 4* ]]; then
  MESSAGE="Released http4k LTS version $1 (See <https://github.com/http4k-lts-v4/http4k/blob/master/CHANGELOG.md|Changelog> for details)."
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$MESSAGE\"}" $LTS_SLACK_WEBHOOK
else
  MESSAGE="Released http4k OSS version $1 (See <h<https://www.http4k.org/ecosystem/http4k/changelog/|Changelog> for details)."
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$MESSAGE\"}" $LTS_SLACK_WEBHOOK
fi
