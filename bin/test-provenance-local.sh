#!/bin/bash
set -euo pipefail

VERSION="${1:-LOCAL}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=== Local Provenance Test (version: $VERSION) ==="

# Check cosign is installed
if ! command -v cosign &> /dev/null; then
    echo "ERROR: cosign not installed. Run: brew install cosign"
    exit 1
fi

# Check jq is installed
if ! command -v jq &> /dev/null; then
    echo "ERROR: jq not installed. Run: brew install jq"
    exit 1
fi

# Generate a throwaway key pair if none provided
if [ -z "${COSIGN_PRIVATE_KEY:-}" ]; then
    echo "=== Generating temporary cosign key pair ==="
    TMPDIR=$(mktemp -d)
    COSIGN_PASSWORD="test" cosign generate-key-pair --output-key-prefix="$TMPDIR/test" 2>/dev/null
    export COSIGN_PRIVATE_KEY=$(cat "$TMPDIR/test.key")
    export COSIGN_PASSWORD="test"
    PUBLIC_KEY="$TMPDIR/test.pub"
    echo "  Temp keys at: $TMPDIR/test.{key,pub}"
fi

# Clean previous runs
rm -f "$REPO_ROOT/build/publish-manifest.txt"
rm -rf "$REPO_ROOT/build/provenance"

# Build a single module to keep it fast
echo ""
echo "=== Building http4k-core ==="
./gradlew :http4k-core:jar --no-configuration-cache -PreleaseVersion="$VERSION" -q

echo ""
echo "=== Generating SBOM for http4k-core ==="
./gradlew :http4k-core:cyclonedxBom --no-configuration-cache -PreleaseVersion="$VERSION" -q

echo ""
echo "=== Building publish manifest ==="
./gradlew :http4k-core:writePublishManifest --no-configuration-cache -PreleaseVersion="$VERSION" -q

echo ""
echo "=== Manifest contents ==="
cat "$REPO_ROOT/build/publish-manifest.txt"

echo ""
echo "=== Signing artifacts and generating provenance ==="
"$REPO_ROOT/bin/sign-and-attest.sh" "$VERSION"

echo ""
echo "=== Generated provenance files ==="
find "$REPO_ROOT/build/provenance" -type f | sort
echo ""
find "$REPO_ROOT/core/core/build/libs" -name "*.sigstore.json" | sort
find "$REPO_ROOT/core/core/build/reports" -name "*.sigstore.json" | sort

# Verify signatures if we have the public key
if [ -n "${PUBLIC_KEY:-}" ]; then
    echo ""
    echo "=== Verifying signatures ==="

    JAR=$(find "$REPO_ROOT/core/core/build/libs" -name "http4k-core-${VERSION}.jar" | head -1)
    if [ -n "$JAR" ] && [ -f "${JAR}.sigstore.json" ]; then
        cosign verify-blob "$JAR" \
            --key "$PUBLIC_KEY" \
            --bundle "${JAR}.sigstore.json" \
            --insecure-ignore-tlog 2>&1 && echo "  JAR signature: VALID" || echo "  JAR signature: FAILED"
    fi

    SBOM="$REPO_ROOT/core/core/build/reports/http4k-core-sbom.json"
    if [ -f "$SBOM" ] && [ -f "${SBOM}.sigstore.json" ]; then
        cosign verify-blob "$SBOM" \
            --key "$PUBLIC_KEY" \
            --bundle "${SBOM}.sigstore.json" \
            --insecure-ignore-tlog 2>&1 && echo "  SBOM signature: VALID" || echo "  SBOM signature: FAILED"
    fi

    PROV=$(find "$REPO_ROOT/build/provenance" -name "*.provenance.json" ! -name "*.sigstore.json" | head -1)
    if [ -n "$PROV" ] && [ -f "${PROV}.sigstore.json" ]; then
        cosign verify-blob "$PROV" \
            --key "$PUBLIC_KEY" \
            --bundle "${PROV}.sigstore.json" \
            --insecure-ignore-tlog 2>&1 && echo "  Provenance signature: VALID" || echo "  Provenance signature: FAILED"
    fi

    echo ""
    echo "=== Provenance content ==="
    cat "$PROV" | jq .
fi

echo ""
echo "=== Done ==="
echo "S3 upload is handled by: ./gradlew uploadProvenance -PreleaseVersion=$VERSION -PltsPublishingUser=... -PltsPublishingPassword=..."
