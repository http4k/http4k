/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class BundleVerifierTest {

    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private val verifier = BundleVerifier(keyPair.public)

    @Test
    fun `verifies valid signature`() {
        val artifact = createTempArtifact()
        val bundle = createBundle(artifact)

        val result = verifier.verify(artifact, bundle)

        assertThat(result.passed, equalTo(true))
        assertThat(result.message, equalTo("Verified OK"))
    }

    @Test
    fun `rejects tampered artifact`() {
        val artifact = createTempArtifact()
        val bundle = createBundle(artifact)

        artifact.writeText("tampered content")

        val result = verifier.verify(artifact, bundle)

        assertThat(result.passed, equalTo(false))
        assertThat(result.message, equalTo("Artifact digest mismatch — file may have been tampered with"))
    }

    @Test
    fun `rejects wrong key`() {
        val otherKeyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        val artifact = createTempArtifact()
        val bundle = createBundle(artifact)

        val result = BundleVerifier(otherKeyPair.public).verify(artifact, bundle)

        assertThat(result.passed, equalTo(false))
        assertThat(result.message, equalTo("Signature verification failed"))
    }

    private fun createTempArtifact(): File =
        File.createTempFile("test-artifact", ".jar").apply {
            deleteOnExit()
            writeText("hello world")
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
          "verificationMaterial": {
            "publicKey": { "hint": "test-key" }
          },
          "messageSignature": {
            "messageDigest": {
              "algorithm": "SHA2_256",
              "digest": "$digestB64"
            },
            "signature": "$signatureB64"
          }
        }
        """.trimIndent()
    }
}
