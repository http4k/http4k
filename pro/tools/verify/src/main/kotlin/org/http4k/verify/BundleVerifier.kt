/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class VerificationResult(val artifact: String, val passed: Boolean, val message: String)

object BundleVerifier {
    fun loadPublicKey(pemContent: String): PublicKey {
        val base64 = pemContent
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
    }

    fun verify(artifact: File, bundleJson: String, publicKey: PublicKey): VerificationResult {
        val name = artifact.name

        val signatureB64 = extractJsonString(bundleJson, "signature")
            ?: extractJsonString(bundleJson, "base64Signature")
            ?: return VerificationResult(name, false, "No signature found in bundle")

        val digestB64 = extractJsonString(bundleJson, "digest")

        val signatureBytes = Base64.getDecoder().decode(signatureB64)

        val artifactBytes = artifact.readBytes()

        if (digestB64 != null) {
            val digestBytes = Base64.getDecoder().decode(digestB64)
            val actualDigest = java.security.MessageDigest.getInstance("SHA-256").digest(artifactBytes)
            if (!actualDigest.contentEquals(digestBytes)) {
                return VerificationResult(name, false, "Artifact digest mismatch — file may have been tampered with")
            }
        }

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(publicKey)
        sig.update(artifactBytes)

        return if (sig.verify(signatureBytes)) {
            VerificationResult(name, true, "Verified OK")
        } else {
            VerificationResult(name, false, "Signature verification failed")
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }
}
