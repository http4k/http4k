#!/bin/bash

source ./release-functions.sh

for i in $(./listProjects.sh); do
    maven_publish "$i"
done
