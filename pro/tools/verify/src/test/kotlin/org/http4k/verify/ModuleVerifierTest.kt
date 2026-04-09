/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class ModuleVerifierTest {

    @TempDir
    lateinit var outputDir: File

    @TempDir
    lateinit var cacheDir: File

    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private val fingerprint = KeyFingerprint.of("sha256:" + keyPair.public.encoded.sha256Hex())

    private val id = testModuleId("org.http4k", "http4k-core", "5.0.0")

    private fun provenanceFile() = createTempFile("""{"predicate":{"signingKey":{"fingerprint":"${fingerprint.value}"}}}""")

    @Test
    fun `verifies jar with bundle`() {
        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile)
        val provenance = provenanceFile()

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { BundleVerifier(keyPair.public) },
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    "provenance" -> provenance
                    else -> null
                }
            }
        )

        val result = verifier.verify(id, jarFile, outputDir)

        assertThat(result.group, equalTo("org.http4k"))
        assertThat(result.module, equalTo("http4k-core"))
        assertThat(result.version, equalTo("5.0.0"))
        assertThat(result.checks[ArtifactType.jar]!!.passed, equalTo(true))
        assertThat(result.checks[ArtifactType.sbom], absent())
        assertThat(result.signingKeyFingerprint, equalTo(fingerprint))
    }

    @Test
    fun `returns cached result on second run`() {
        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile)
        val provenance = provenanceFile()

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { BundleVerifier(keyPair.public) },
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    "provenance" -> provenance
                    else -> null
                }
            }
        )

        verifier.verify(id, jarFile, outputDir)
        val second = verifier.verify(id, jarFile, outputDir)

        assertThat(second.checks[ArtifactType.jar]!!.message, equalTo("cached"))
    }

    @Test
    fun `exports files to output directory`() {
        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile)
        val provenance = provenanceFile()

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { BundleVerifier(keyPair.public) },
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    "provenance" -> provenance
                    else -> null
                }
            }
        )

        verifier.verify(id, jarFile, outputDir)

        val moduleDir = File(outputDir, "org.http4k/http4k-core/5.0.0")
        assertThat(moduleDir.exists(), equalTo(true))
        assertThat(File(moduleDir, "http4k-core-5.0.0.jar.sha256").exists(), equalTo(true))
        assertThat(File(moduleDir, "http4k-core-5.0.0-jar-sigstore.json").exists(), equalTo(true))
    }

    @Test
    fun `records failure for tampered artifact`() {
        val jarFile = createTempFile("original content")
        val jarBundle = createBundle(jarFile)
        jarFile.writeText("tampered content")
        val provenance = provenanceFile()

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { BundleVerifier(keyPair.public) },
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    "provenance" -> provenance
                    else -> null
                }
            }
        )

        val result = verifier.verify(id, jarFile, outputDir)

        assertThat(result.checks[ArtifactType.jar]!!.passed, equalTo(false))
    }

    @Test
    fun `selects correct key from provenance fingerprint`() {
        val keyPair2 = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        val fingerprint2 = KeyFingerprint.of("sha256:" + keyPair2.public.encoded.sha256Hex())

        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile, keyPair2)
        val provenanceContent = """{"predicate":{"signingKey":{"fingerprint":"${fingerprint2.value}"}}}"""
        val provenance = createTempFile(provenanceContent)
        val provenanceBundle = createBundle(provenance, keyPair2)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            resolveBundleVerifier = { fp ->
                when (fp) {
                    fingerprint2 -> BundleVerifier(keyPair2.public)
                    else -> BundleVerifier(keyPair.public)
                }
            },
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    "provenance" -> provenance
                    "provenance-sigstore" -> createTempFile(provenanceBundle)
                    else -> null
                }
            }
        )

        val result = verifier.verify(id, jarFile, outputDir)

        assertThat(result.checks[ArtifactType.jar]!!.passed, equalTo(true))
        assertThat(result.checks[ArtifactType.provenance]!!.passed, equalTo(true))
        assertThat(result.signingKeyFingerprint, equalTo(fingerprint2))
    }

    private fun createTempFile(content: String) =
        File.createTempFile("test", ".tmp").apply {
            deleteOnExit()
            writeText(content)
        }

    private fun createBundle(artifact: File, kp: java.security.KeyPair = keyPair): String {
        val artifactBytes = artifact.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(artifactBytes)
        val digestB64 = Base64.getEncoder().encodeToString(digest)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(kp.private)
        sig.update(artifactBytes)
        val signatureB64 = Base64.getEncoder().encodeToString(sig.sign())

        return """
        {
          "mediaType": "application/vnd.dev.sigstore.bundle.v0.3+json",
          "verificationMaterial": { "publicKey": { "hint": "test-key" } },
          "messageSignature": {
            "messageDigest": { "algorithm": "SHA2_256", "digest": "$digestB64" },
            "signature": "$signatureB64"
          }
        }
        """.trimIndent()
    }
}

internal fun testModuleId(group: String, module: String, version: String) =
    object : ModuleComponentIdentifier {
        override fun getGroup() = group
        override fun getModule() = module
        override fun getVersion() = version
        override fun getModuleIdentifier() = object : org.gradle.api.artifacts.ModuleIdentifier {
            override fun getGroup() = group
            override fun getName() = module
        }
        override fun getDisplayName() = "$group:$module:$version"
    }
