#!/bin/bash
set -e

function replace() {
    TARGET_FILE=$1
    SOURCE=$2
    REPLACEMENT=$3
    cat "$TARGET_FILE" | grep -v "$SOURCE" > "$TARGET_FILE".tmp
    echo "$REPLACEMENT" >> "$TARGET_FILE".tmp
    mv "$TARGET_FILE".tmp "$TARGET_FILE"
}

export -f upgrade

find . -name $1 -exec bash -c "replace {} $2 $3" bash {} \;
