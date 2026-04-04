/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `verifies valid signature`() {
        val artifact = createTempArtifact("hello world")
        val bundle = createBundle(artifact)

        val result = BundleVerifier.verify(artifact, bundle, keyPair.public)

        assertTrue(result.passed)
        assertEquals("Verified OK", result.message)
    }

    @Test
    fun `rejects tampered artifact`() {
        val artifact = createTempArtifact("hello world")
        val bundle = createBundle(artifact)

        artifact.writeText("tampered content")

        val result = BundleVerifier.verify(artifact, bundle, keyPair.public)

        assertFalse(result.passed)
        assertEquals("Artifact digest mismatch — file may have been tampered with", result.message)
    }

    @Test
    fun `rejects wrong key`() {
        val otherKeyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        val artifact = createTempArtifact("hello world")
        val bundle = createBundle(artifact)

        val result = BundleVerifier.verify(artifact, bundle, otherKeyPair.public)

        assertFalse(result.passed)
        assertEquals("Signature verification failed", result.message)
    }

    @Test
    fun `loads PEM public key`() {
        val encoded = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val pem = "-----BEGIN PUBLIC KEY-----\n$encoded\n-----END PUBLIC KEY-----"

        val key = BundleVerifier.loadPublicKey(pem)
        assertEquals("EC", key.algorithm)
    }

    private fun createTempArtifact(content: String): File =
        File.createTempFile("test-artifact", ".jar").apply {
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
