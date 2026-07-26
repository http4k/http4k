#!/bin/bash
set -euo pipefail

# Runs in the build job before publishAllPublicationsToHttp4kLtsRepository. Downloads each
# published module's existing maven-metadata.xml from S3 into the build/lts-staging layout so
# Gradle merges the new release into the historical version list (instead of overwriting it).
#
# Uses per-object access (needs only s3:GetObject, which the publishing creds already have)
# rather than `aws s3 sync` (which would require s3:ListBucket on the whole bucket).
#
# SAFETY: a missing object (clean 404) is a brand-new module and is fine — Gradle then writes a
# fresh single-version file. ANY OTHER error (403 / wrong region / network) is fatal: if we
# couldn't read the existing metadata we must NOT proceed, because the later `aws s3 sync` would
# overwrite the real multi-version metadata with a single-version one and destroy version history.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$REPO_ROOT/build/publish-manifest.txt"
LAYOUT="$REPO_ROOT/build/lts-staging"
BUCKET="${HTTP4K_MAVEN_BUCKET:-http4k-maven}"

[ -f "$MANIFEST" ] || { echo "ERROR: $MANIFEST not found (run writePublishManifest first)." >&2; exit 1; }

seeded=0
new=0
while IFS='|' read -r GROUP ARTIFACT_ID _MODULE_VERSION _BUILD_DIR; do
    GROUP_PATH="${GROUP//.//}"
    KEY="$GROUP_PATH/$ARTIFACT_ID/maven-metadata.xml"
    DEST="$LAYOUT/$GROUP_PATH/$ARTIFACT_ID"
    mkdir -p "$DEST"

    if err=$(aws s3api head-object --bucket "$BUCKET" --key "$KEY" 2>&1 >/dev/null); then
        aws s3 cp "s3://$BUCKET/$KEY" "$DEST/maven-metadata.xml" >/dev/null
        seeded=$((seeded + 1))
    else
        case "$err" in
            *"Not Found"*|*404*) new=$((new + 1)) ;;  # brand-new module — fine
            *) echo "ERROR: cannot read s3://$BUCKET/$KEY (not a 404): $err" >&2
               echo "Refusing to continue — publishing would overwrite existing metadata." >&2
               exit 1 ;;
        esac
    fi
done < "$MANIFEST"

echo "Pre-seeded existing maven-metadata.xml for $seeded module(s); $new brand-new module(s)."
