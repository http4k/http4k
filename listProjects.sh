#!/bin/bash

./gradlew listProjects -q 2> errors.txt
cat errors.txt
rm errors.txt
