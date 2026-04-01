#!/bin/bash
set -euo pipefail

VERSION="$1"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$REPO_ROOT/build/publish-manifest.txt"
PROVENANCE_DIR="$REPO_ROOT/build/provenance"

if [ ! -f "$MANIFEST" ]; then
    echo "ERROR: publish-manifest.txt not found. Run ./gradlew writePublishManifest first."
    exit 1
fi

mkdir -p "$PROVENANCE_DIR"

GIT_COMMIT=$(git -C "$REPO_ROOT" rev-parse HEAD)
BUILD_TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)

COSIGN_MAJOR=$(cosign version 2>&1 | sed -n 's/.*GitVersion:[[:space:]]*v\([0-9]*\).*/\1/p')
COSIGN_MAJOR="${COSIGN_MAJOR:-2}"

echo "Detected cosign major version: $COSIGN_MAJOR"

if [[ "$COSIGN_MAJOR" -ge 3 ]]; then
    SIGNING_CONFIG="$REPO_ROOT/build/signing-config.json"
    cat > "$SIGNING_CONFIG" <<'CFGEOF'
{
  "mediaType": "application/vnd.dev.sigstore.signingconfig.v0.2+json",
  "caUrls": [],
  "oidcUrls": [],
  "tsaUrls": [
    {
      "url": "https://timestamp.sigstore.dev/api/v1/timestamp",
      "majorApiVersion": 1,
      "validFor": { "start": "2025-07-04T00:00:00Z" },
      "operator": "sigstore.dev"
    }
  ],
  "rekorTlogConfig": { "selector": "ANY" },
  "tsaConfig": { "selector": "ANY" }
}
CFGEOF
    TLOG_FLAG="--signing-config $SIGNING_CONFIG"
else
    TLOG_FLAG="--tlog-upload=false --timestamp-server-url=https://timestamp.sigstore.dev/api/v1/timestamp"
fi

sign_blob() {
    local file="$1"
    local bundle="${file}.sigstore.json"
    cosign sign-blob "$file" \
        --key env://COSIGN_PRIVATE_KEY \
        $TLOG_FLAG \
        --bundle "$bundle" \
        --yes
    echo "  Signed: $(basename "$bundle")"
}

while IFS='|' read -r GROUP ARTIFACT_ID MODULE_VERSION BUILD_DIR; do
    echo "Processing $GROUP:$ARTIFACT_ID:$MODULE_VERSION"

    SUBJECTS="[]"
    JAR_DIR="$BUILD_DIR/libs"

    if [ -d "$JAR_DIR" ]; then
        for jar in "$JAR_DIR"/*.jar; do
            [ -f "$jar" ] || continue
            sign_blob "$jar"

            SHA256=$(sha256sum "$jar" | awk '{print $1}')
            JAR_NAME=$(basename "$jar")
            SUBJECTS=$(echo "$SUBJECTS" | jq --arg name "$JAR_NAME" --arg sha "$SHA256" \
                '. + [{"name": $name, "digest": {"sha256": $sha}}]')
        done
    fi

    SBOM_FILE="$BUILD_DIR/reports/${ARTIFACT_ID}-sbom.json"
    if [ -f "$SBOM_FILE" ]; then
        sign_blob "$SBOM_FILE"
    fi

    PROVENANCE_FILE="$PROVENANCE_DIR/${ARTIFACT_ID}-${MODULE_VERSION}.provenance.json"
    cat > "$PROVENANCE_FILE" <<PROVEOF
{
  "_type": "https://in-toto.io/Statement/v1",
  "subject": $SUBJECTS,
  "predicateType": "https://slsa.dev/provenance/v1",
  "predicate": {
    "buildDefinition": {
      "buildType": "https://github.com/http4k/http4k/blob/main/.github/workflows/publish-artifacts.yml",
      "externalParameters": {
        "tag": "$MODULE_VERSION"
      },
      "internalParameters": {},
      "resolvedDependencies": [
        {
          "uri": "git+https://github.com/http4k/http4k@refs/tags/$MODULE_VERSION",
          "digest": {
            "gitCommit": "$GIT_COMMIT"
          }
        }
      ]
    },
    "runDetails": {
      "builder": {
        "id": "https://github.com/http4k/http4k/blob/main/.github/workflows/publish-artifacts.yml"
      },
      "metadata": {
        "invocationId": "${GITHUB_RUN_ID:-local}",
        "startedOn": "$BUILD_TIMESTAMP"
      }
    }
  }
}
PROVEOF

    sign_blob "$PROVENANCE_FILE"

done < "$MANIFEST"

echo "Signing and attestation complete."
