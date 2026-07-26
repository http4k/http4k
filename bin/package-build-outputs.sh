#!/bin/bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$REPO_ROOT/build/publish-manifest.txt"
LAYOUT="$REPO_ROOT/build/lts-staging"

[ -f "$MANIFEST" ] || { echo "ERROR: $MANIFEST not found (run writePublishManifest first)." >&2; exit 1; }
[ -d "$LAYOUT" ]   || { echo "ERROR: $LAYOUT not found (run publishAllPublicationsToHttp4kLtsRepository first)." >&2; exit 1; }

while IFS='|' read -r GROUP ARTIFACT_ID MODULE_VERSION BUILD_DIR; do
    GROUP_PATH="${GROUP//.//}"
    VDIR="$LAYOUT/$GROUP_PATH/$ARTIFACT_ID/$MODULE_VERSION"
    PREFIX="$ARTIFACT_ID-$MODULE_VERSION"
    [ -d "$VDIR" ] || { echo "ERROR: expected layout dir missing: $VDIR" >&2; exit 1; }

    SBOM="$BUILD_DIR/reports/${ARTIFACT_ID}-sbom.json"
    [ -f "$SBOM" ] && cp "$SBOM" "$VDIR/$PREFIX-cyclonedx.json"

    LICENSE="$BUILD_DIR/reports/${ARTIFACT_ID}-license-report.json"
    [ -f "$LICENSE" ] && cp "$LICENSE" "$VDIR/$PREFIX-license-report.json"
done < "$MANIFEST"

cd "$REPO_ROOT"
tar -czf build-outputs.tar.gz build/lts-staging build/publish-manifest.txt
echo "Packaged $(wc -l < "$MANIFEST" | tr -d ' ') modules into build-outputs.tar.gz"
