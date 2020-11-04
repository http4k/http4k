#!/bin/bash

for i in $(./gradlew listProjects -q > /dev/null
2>&1); do
    echo "$i"
done
