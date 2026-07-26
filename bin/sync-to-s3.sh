#!/bin/bash
set -euo pipefail

# Pushes the signed build/lts-staging Maven layout to S3 (runs in the isolated attest job, no Gradle).
#
# Artifacts (jars, poms, sigstore/provenance/sbom/license, .asc, checksums) sync in bulk. The
# artifact-level maven-metadata.xml is handled separately: only the module coordinates in the
# manifest have metadata that Gradle merged against S3 (via bin/preseed-metadata.sh), so only those
# are pushed. Metadata for coordinates NOT in the manifest — notably Gradle plugin markers
# (org.http4k.<id>.gradle.plugin) — was written single-version by the local publish and must NOT be
# pushed, or it would overwrite the real multi-version history on S3. Those keep their existing S3
# metadata; the new version is still resolvable by exact coordinate from its published POM.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LAYOUT="$REPO_ROOT/build/lts-staging"
MANIFEST="$REPO_ROOT/build/publish-manifest.txt"
BUCKET="${HTTP4K_MAVEN_BUCKET:-s3://http4k-maven}"

[ -d "$LAYOUT" ]   || { echo "ERROR: $LAYOUT not found." >&2; exit 1; }
[ -f "$MANIFEST" ] || { echo "ERROR: $MANIFEST not found." >&2; exit 1; }

# 1. Everything except maven-metadata.xml (+ its checksums).
aws s3 sync "$LAYOUT" "$BUCKET" --exclude '*maven-metadata.xml*'

# 2. Module-level maven-metadata.xml (Gradle-merged) only, per manifest coordinate.
while IFS='|' read -r GROUP ARTIFACT_ID _MODULE_VERSION _BUILD_DIR; do
    GROUP_PATH="${GROUP//.//}"
    for ext in "" .md5 .sha1 .sha256 .sha512; do
        f="$LAYOUT/$GROUP_PATH/$ARTIFACT_ID/maven-metadata.xml$ext"
        [ -f "$f" ] && aws s3 cp "$f" "$BUCKET/$GROUP_PATH/$ARTIFACT_ID/maven-metadata.xml$ext"
    done
done < "$MANIFEST"

echo "S3 sync complete."
