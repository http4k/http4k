#!/usr/bin/env bash

set -e
set -o errexit
set -o pipefail
set -o nounset

python3 -m venv .venv
source .venv/bin/activate

pip3 install -r requirements.txt

cd src
mkdocs serve
