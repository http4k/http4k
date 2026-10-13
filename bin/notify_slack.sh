#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

if [[ $1 == *-ee ]]; then
  MESSAGE="Released http4k Enterprise Edition version $1 (See <https://github.com/http4k/http4k/blob/ee/CHANGELOG.md|Changelog> for details)."
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$MESSAGE\"}" $SLACK_WEBHOOK
elif [[ $1 == *-lts ]]; then
  MESSAGE="Released http4k LTS version $1 (See <https://github.com/http4k/http4k/blob/lts/CHANGELOG.md|Changelog> for details)."
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$MESSAGE\"}" $SLACK_WEBHOOK
else
  MESSAGE="Released http4k Community version $1 (See <https://github.com/http4k/http4k/blob/master/CHANGELOG.md|Changelog> for details)."
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$MESSAGE\"}" $SLACK_WEBHOOK
fi
