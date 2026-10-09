#!/bin/bash
set -euo pipefail

# Gradle sets <latest>/<release> in maven-metadata.xml to the version just published. With several
# release lines (e.g. a v6 patch published after v7) that would point them backwards, so reset both
# to the highest listed version and regenerate whichever checksums Gradle wrote alongside.

for f in "$@"; do
    top=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$f" | sort -V | tail -1)
    [ -n "$top" ] || { echo "ERROR: no versions in $f" >&2; exit 1; }
    sed -e "s:<latest>.*</latest>:<latest>$top</latest>:" \
        -e "s:<release>.*</release>:<release>$top</release>:" "$f" > "$f.tmp"
    mv "$f.tmp" "$f"
    for alg in md5 sha1 sha256 sha512; do
        if [ -f "$f.$alg" ]; then openssl dgst -"$alg" -r "$f" | cut -d' ' -f1 > "$f.$alg"; fi
    done
done
