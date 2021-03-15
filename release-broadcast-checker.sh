#!/bin/bash

LOCAL_VERSION=$(tools/jq -r .http4k.version ./version.json)

S3_VERSION=$(aws s3 cp s3://http4k/latest-broadcasted-version.txt -)

if [[ $S3_VERSION == $LOCAL_VERSION ]]; then
    echo "Version $LOCAL_VERSION has been broadcasted already."
    echo "::set-output name=requires-broadcast::false"
    exit 0
fi;

echo "Latest broadcasted version was ${S3_VERSION}. Checking for ${LOCAL_VERSION} in maven central..."

MC_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "https://repo1.maven.org/maven2/org/http4k/http4k-core/${LOCAL_VERSION}/http4k-core-${LOCAL_VERSION}.pom"
)

if [[ $MC_STATUS == "200" ]]; then
    echo "Version $LOCAL_VERSION available in MC. Preparing for broadcast..."
    echo "${LOCAL_VERSION}" | aws s3 cp - s3://http4k/latest-broadcasted-version.txt
    echo "::set-output name=requires-broadcast::true"
fi;
