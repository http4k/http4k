#!/bin/bash

./gradlew listProjects -q 2> projects.txt
cat projects.txt
rm projects.txt
