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

    private val id = testModuleId("org.http4k", "http4k-core", "5.0.0")

    @Test
    fun `verifies jar with bundle`() {
        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            bundleVerifier = BundleVerifier(keyPair.public),
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
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
    }

    @Test
    fun `returns cached result on second run`() {
        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            bundleVerifier = BundleVerifier(keyPair.public),
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    else -> null
                }
            }
        )

        verifier.verify(id, jarFile, outputDir)
        val second = verifier.verify(id, jarFile, outputDir)

        assertThat(second.checks[ArtifactType.jar]!!.message, equalTo("cached"))
    }

    @Test
    fun `skips types with no bundle`() {
        val jarFile = createTempFile("hello jar")

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            bundleVerifier = BundleVerifier(keyPair.public),
            resolveClassified = { _, _ -> null }
        )

        val result = verifier.verify(id, jarFile, outputDir)

        ArtifactType.entries.forEach { type ->
            assertThat(result.checks[type], absent())
        }
    }

    @Test
    fun `exports files to output directory`() {
        val jarFile = createTempFile("hello jar")
        val jarBundle = createBundle(jarFile)

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            bundleVerifier = BundleVerifier(keyPair.public),
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
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

        val verifier = ModuleVerifier(
            cache = VerificationCache(cacheDir),
            bundleVerifier = BundleVerifier(keyPair.public),
            resolveClassified = { _, classifier ->
                when (classifier) {
                    "jar-sigstore" -> createTempFile(jarBundle)
                    else -> null
                }
            }
        )

        val result = verifier.verify(id, jarFile, outputDir)

        assertThat(result.checks[ArtifactType.jar]!!.passed, equalTo(false))
    }

    private fun createTempFile(content: String) =
        File.createTempFile("test", ".tmp").apply {
            deleteOnExit()
            writeText(content)
        }

    private fun createBundle(artifact: File): String {
        val artifactBytes = artifact.readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(artifactBytes)
        val digestB64 = Base64.getEncoder().encodeToString(digest)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private)
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

private fun testModuleId(group: String, module: String, version: String) =
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
