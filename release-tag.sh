#!/bin/bash
set -e

source ./release-functions.sh

ensure_release_commit

create_tag

