#!/bin/bash
set -e

function upgrade {
    echo Upgrade $1 to $2
    find . -type f -name '*.md' | grep -v "node_modules" | grep -v "bower_components" | xargs sed -i '' s/"$1"/"$2"/g
    sed -i '' s/"$1"/"$2"/g build.sbt
}

upgrade `./tools/jq .reekwest.old version.json` `./tools/jq .reekwest.new version.json`
