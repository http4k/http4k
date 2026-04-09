/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64

class EndToEndVerificationTest {

    @TempDir
    lateinit var outputDir: File

    @TempDir
    lateinit var cacheDir: File

    @TempDir
    lateinit var artifactDir: File

    private val signingKey = generateKeyPair()
    private val oldKey = generateKeyPair()

    private val signingFingerprint = fingerprint(signingKey)
    private val oldFingerprint = fingerprint(oldKey)

    private val keyList = CosignKeyList(
        schemaVersion = 1,
        keys = listOf(
            CosignKey(
                kid = "http4k-signing-2025",
                fingerprint = signingFingerprint.value,
                publicKey = toPem(signingKey),
                status = KeyStatus.active,
                validFrom = Instant.parse("2025-01-01T00:00:00Z")
            ),
            CosignKey(
                kid = "http4k-signing-2024",
                fingerprint = oldFingerprint.value,
                publicKey = toPem(oldKey),
                status = KeyStatus.retired,
                validFrom = Instant.parse("2024-01-01T00:00:00Z"),
                validTo = Instant.parse("2025-01-01T00:00:00Z")
            )
        )
    )

    @Test
    fun `verifies all artifact types with key resolved from provenance fingerprint`() {
        val jar = createArtifact("jar content")
        val sbom = createArtifact("""{"bomFormat":"CycloneDX"}""")
        val license = createArtifact("""{"licenses":[]}""")
        val provenance = createArtifact(provenanceJson(signingFingerprint, jar))

        val artifacts = mapOf(
            "jar-sigstore" to createBundleFile(jar, signingKey),
            "cyclonedx" to sbom,
            "cyclonedx-sigstore" to createBundleFile(sbom, signingKey),
            "provenance" to provenance,
            "provenance-sigstore" to createBundleFile(provenance, signingKey),
            "license-report" to license,
            "license-report-sigstore" to createBundleFile(license, signingKey)
        )

        val keyResolver = KeyResolver(keyList)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { fp -> BundleVerifier(keyResolver.resolve(fp).key) },
            resolveClassified = { _, classifier -> artifacts[classifier] }
        )

        val result = verifier.verify(testModuleId("org.http4k", "http4k-core", "5.0.0"), jar, outputDir)

        assertThat(result.allPassed, equalTo(true))
        assertThat(result.verified, equalTo(4))
        assertThat(result.signingKeyFingerprint, equalTo(signingFingerprint))

        ArtifactType.entries.forEach { type ->
            assertThat("${type.name} should pass", result.checks[type]!!.passed, equalTo(true))
        }
    }

    @Test
    fun `verifies artifacts signed with old retired key`() {
        val jar = createArtifact("old jar content")
        val provenance = createArtifact(provenanceJson(oldFingerprint, jar))

        val artifacts = mapOf(
            "jar-sigstore" to createBundleFile(jar, oldKey),
            "provenance" to provenance,
            "provenance-sigstore" to createBundleFile(provenance, oldKey)
        )

        val keyResolver = KeyResolver(keyList)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { fp -> BundleVerifier(keyResolver.resolve(fp).key) },
            resolveClassified = { _, classifier -> artifacts[classifier] }
        )

        val result = verifier.verify(testModuleId("org.http4k", "http4k-core", "4.0.0"), jar, outputDir)

        assertThat(result.checks[ArtifactType.jar]!!.passed, equalTo(true))
        assertThat(result.checks[ArtifactType.provenance]!!.passed, equalTo(true))
        assertThat(result.signingKeyFingerprint, equalTo(oldFingerprint))
    }

    @Test
    fun `rejects artifact signed with revoked key`() {
        val revokedKey = generateKeyPair()
        val revokedFingerprint = fingerprint(revokedKey)

        val revokedKeyList = CosignKeyList(
            schemaVersion = 1,
            keys = listOf(
                CosignKey(
                    kid = "http4k-signing-revoked",
                    fingerprint = revokedFingerprint.value,
                    publicKey = toPem(revokedKey),
                    status = KeyStatus.revoked,
                    validFrom = Instant.parse("2023-01-01T00:00:00Z")
                )
            )
        )

        val jar = createArtifact("bad jar")
        val provenance = createArtifact(provenanceJson(revokedFingerprint, jar))

        val artifacts = mapOf(
            "jar-sigstore" to createBundleFile(jar, revokedKey),
            "provenance" to provenance
        )

        val keyResolver = KeyResolver(revokedKeyList)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { fp -> BundleVerifier(keyResolver.resolve(fp).key) },
            resolveClassified = { _, classifier -> artifacts[classifier] }
        )

        assertThat(
            {
                verifier.verify(testModuleId("org.http4k", "http4k-core", "3.0.0"), jar, outputDir)
            },
            throws<KeyRevokedException>()
        )
    }

    @Test
    fun `detects tampered jar`() {
        val jar = createArtifact("original content")
        val jarBundle = createBundleFile(jar, signingKey)
        jar.writeText("tampered content")

        val provenance = createArtifact(provenanceJson(signingFingerprint, jar))

        val artifacts = mapOf(
            "jar-sigstore" to jarBundle,
            "provenance" to provenance,
            "provenance-sigstore" to createBundleFile(provenance, signingKey)
        )

        val keyResolver = KeyResolver(keyList)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { fp -> BundleVerifier(keyResolver.resolve(fp).key) },
            resolveClassified = { _, classifier -> artifacts[classifier] }
        )

        val result = verifier.verify(testModuleId("org.http4k", "http4k-core", "5.0.0"), jar, outputDir)

        assertThat(result.checks[ArtifactType.jar]!!.passed, equalTo(false))
        assertThat(result.checks[ArtifactType.provenance]!!.passed, equalTo(true))
    }

    private fun provenanceJson(fingerprint: KeyFingerprint, jar: File) = """
    {
      "_type": "https://in-toto.io/Statement/v1",
      "subject": [{"name": "${jar.name}", "digest": {"sha256": "${jar.sha256Hex()}"}}],
      "predicateType": "https://slsa.dev/provenance/v1",
      "predicate": {
        "buildDefinition": {"buildType": "test", "externalParameters": {}, "internalParameters": {}, "resolvedDependencies": []},
        "runDetails": {"builder": {"id": "test"}, "metadata": {"invocationId": "test", "startedOn": "2025-01-01T00:00:00Z"}},
        "signingKey": {"fingerprint": "${fingerprint.value}"}
      }
    }
    """.trimIndent()

    private var artifactCounter = 0

    private fun createArtifact(content: String) =
        File(artifactDir, "artifact-${artifactCounter++}.tmp").apply { writeText(content) }

    private fun createBundleFile(artifact: File, kp: KeyPair): File {
        val artifactBytes = artifact.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(artifactBytes)
        val digestB64 = Base64.getEncoder().encodeToString(digest)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(kp.private)
        sig.update(artifactBytes)
        val signatureB64 = Base64.getEncoder().encodeToString(sig.sign())

        val bundle = """
        {
          "mediaType": "application/vnd.dev.sigstore.bundle.v0.3+json",
          "verificationMaterial": {"publicKey": {"hint": "test-key"}},
          "messageSignature": {
            "messageDigest": {"algorithm": "SHA2_256", "digest": "$digestB64"},
            "signature": "$signatureB64"
          }
        }
        """.trimIndent()

        return File(artifactDir, "bundle-${artifactCounter++}.json").apply { writeText(bundle) }
    }

    private fun generateKeyPair() =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    private fun fingerprint(kp: KeyPair) = KeyFingerprint.of("sha256:" + kp.public.encoded.sha256Hex())

    private fun toPem(kp: KeyPair) =
        "-----BEGIN PUBLIC KEY-----\n${Base64.getEncoder().encodeToString(kp.public.encoded)}\n-----END PUBLIC KEY-----"
}
