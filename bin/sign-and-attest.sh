#!/bin/bash
set -euo pipefail

# SLSA L3 sign + attest — runs in the isolated (Gradle-free) publish job.
#
# Operates on the Maven layout produced by `publishAllPublicationsToHttp4kLtsRepository`
# (build/lts-staging). For every published module it:
#   - cosign-signs each jar / pom / sbom / license-report  -> <a>-<v>-<classifier>-sigstore.json
#   - builds + cosign-signs a SLSA provenance statement     -> <a>-<v>-provenance(.sigstore).json
#   - GPG detach-signs every placed "extra" (.asc)          (base jars/pom were signed in job 1)
#   - writes md5/sha1/sha256/sha512 for every placed file
# The whole layout is then pushed to S3 with `aws s3 sync` (no Gradle).
#
# The build job's SBOM/license JSON are copied into the layout as the -cyclonedx / -license-report
# classified artifacts by bin/package-build-outputs.sh before this script runs.

VERSION="$1"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$REPO_ROOT/build/publish-manifest.txt"
LAYOUT="$REPO_ROOT/build/lts-staging"

[ -f "$MANIFEST" ] || { echo "ERROR: $MANIFEST not found (run writePublishManifest in the build job)." >&2; exit 1; }
[ -d "$LAYOUT" ]   || { echo "ERROR: $LAYOUT not found (run publishAllPublicationsToHttp4kLtsRepository)." >&2; exit 1; }

GIT_COMMIT=$(git -C "$REPO_ROOT" rev-parse HEAD)
BUILD_TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# --- GPG key import (extras are signed here, not by Gradle) ------------------------------------
if [ -n "${SIGNING_KEY:-}" ]; then
    { printf '%s' "$SIGNING_KEY" | gpg --batch --import 2>/dev/null; } \
        || { printf '%s' "$SIGNING_KEY" | base64 -d | gpg --batch --import; }
fi

sign_gpg() {
    local file="$1"
    gpg --batch --yes --pinentry-mode loopback --passphrase "${SIGNING_PASSWORD:-}" \
        --detach-sign --armor "$file"
}

checksums() {
    local file="$1"
    md5sum    "$file" | awk '{print $1}' > "$file.md5"
    sha1sum   "$file" | awk '{print $1}' > "$file.sha1"
    sha256sum "$file" | awk '{print $1}' > "$file.sha256"
    sha512sum "$file" | awk '{print $1}' > "$file.sha512"
}

# --- cosign version / signing config ----------------------------------------------------------
COSIGN_KEY_FINGERPRINT="sha256:$(cosign public-key --key env://COSIGN_PRIVATE_KEY 2>/dev/null | openssl pkey -pubin -outform DER 2>/dev/null | sha256sum | awk '{print $1}')"
echo "Signing key fingerprint: $COSIGN_KEY_FINGERPRINT"

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

# cosign sign-blob $1 -> bundle $2 ; then GPG-sign the bundle + checksums for both.
sign_blob() {
    local file="$1"
    local bundle="$2"
    local extra_flags=""
    if [[ "$COSIGN_MAJOR" -lt 3 ]]; then
        extra_flags="--rfc3161-timestamp=${bundle}.rfc3161.timestamp"
    fi
    cosign sign-blob "$file" \
        --key env://COSIGN_PRIVATE_KEY \
        $TLOG_FLAG \
        --bundle "$bundle" \
        $extra_flags \
        --yes
    sign_gpg "$bundle"
    checksums "$bundle"
    echo "  Signed: $(basename "$bundle")"
}

# GPG-sign + checksum a placed data extra (sbom / license / provenance) that carries no cosign
# bundle of its own but still needs an .asc, matching the pre-L3 publication behaviour.
publish_extra() {
    local file="$1"
    sign_gpg "$file"
    checksums "$file"
}

while IFS='|' read -r GROUP ARTIFACT_ID MODULE_VERSION _BUILD_DIR; do
    GROUP_PATH="${GROUP//.//}"
    VDIR="$LAYOUT/$GROUP_PATH/$ARTIFACT_ID/$MODULE_VERSION"
    PREFIX="$ARTIFACT_ID-$MODULE_VERSION"
    [ -d "$VDIR" ] || { echo "ERROR: expected layout dir missing: $VDIR" >&2; exit 1; }

    echo "Processing $GROUP:$ARTIFACT_ID:$MODULE_VERSION"

    SUBJECTS="[]"

    # jars -> <prefix>-<classifier>-sigstore.json  (classifier: jar|sources|javadoc|test-fixtures-sources)
    for jar in "$VDIR/$PREFIX".jar "$VDIR/$PREFIX"-*.jar; do
        [ -f "$jar" ] || continue
        stem="$(basename "$jar" .jar)"
        suffix="${stem#"$PREFIX"}"
        cls="jar"; [ -n "$suffix" ] && cls="${suffix#-}"
        sign_blob "$jar" "$VDIR/$PREFIX-$cls-sigstore.json"

        SHA256=$(sha256sum "$jar" | awk '{print $1}')
        SUBJECTS=$(echo "$SUBJECTS" | jq --arg name "$(basename "$jar")" --arg sha "$SHA256" \
            '. + [{"name": $name, "digest": {"sha256": $sha}}]')
    done

    # pom -> <prefix>-pom-sigstore.json
    [ -f "$VDIR/$PREFIX.pom" ] && sign_blob "$VDIR/$PREFIX.pom" "$VDIR/$PREFIX-pom-sigstore.json"

    # sbom (placed as <prefix>-cyclonedx.json by package-build-outputs.sh)
    if [ -f "$VDIR/$PREFIX-cyclonedx.json" ]; then
        publish_extra "$VDIR/$PREFIX-cyclonedx.json"
        sign_blob "$VDIR/$PREFIX-cyclonedx.json" "$VDIR/$PREFIX-cyclonedx-sigstore.json"
    fi

    # license report (placed as <prefix>-license-report.json)
    if [ -f "$VDIR/$PREFIX-license-report.json" ]; then
        publish_extra "$VDIR/$PREFIX-license-report.json"
        sign_blob "$VDIR/$PREFIX-license-report.json" "$VDIR/$PREFIX-license-report-sigstore.json"
    fi

    # provenance statement (built here) -> <prefix>-provenance(.sigstore).json
    PROVENANCE_FILE="$VDIR/$PREFIX-provenance.json"
    cat > "$PROVENANCE_FILE" <<PROVEOF
{
  "_type": "https://in-toto.io/Statement/v1",
  "subject": $SUBJECTS,
  "predicateType": "https://slsa.dev/provenance/v1",
  "predicate": {
    "buildDefinition": {
      "buildType": "https://github.com/http4k/http4k/blob/$GIT_COMMIT/.github/workflows/publish-artifacts.yml",
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
        "id": "https://github.com/http4k/http4k/blob/$GIT_COMMIT/.github/workflows/publish-artifacts.yml"
      },
      "metadata": {
        "invocationId": "${GITHUB_RUN_ID:-local}",
        "startedOn": "$BUILD_TIMESTAMP"
      }
    },
    "signingKey": {
      "fingerprint": "$COSIGN_KEY_FINGERPRINT"
    }
  }
}
PROVEOF
    publish_extra "$PROVENANCE_FILE"
    sign_blob "$PROVENANCE_FILE" "$VDIR/$PREFIX-provenance-sigstore.json"

done < "$MANIFEST"

echo "Signing and attestation complete."
