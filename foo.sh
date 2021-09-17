#!/bin/bash

for file in $(find . -name 'build.gradle')
do
  mv $file $(echo "$file" | sed -r 's|gradle|gradle.kts|g')
done
