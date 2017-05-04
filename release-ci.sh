#!/bin/bash

LOCAL_VERSION=`./tools/jq -r .reekwest.new version.json`

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/reekwest/maven/reekwest/versions/_latest | ./tools/jq -r .name`

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed"
    exit 0
fi

echo "Version has changed. A new release should be triggered here..."