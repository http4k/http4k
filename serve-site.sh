#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

pip install -r requirements.txt

cd src
mkdocs serve
