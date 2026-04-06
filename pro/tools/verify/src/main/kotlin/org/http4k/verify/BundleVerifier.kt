/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.format.Moshi
import java.io.File
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

data class VerificationResult(val artifact: String, val passed: Boolean, val message: String)

private data class MessageDigestInfo(val digest: String? = null)
private data class MessageSignature(
    val signature: String? = null,
    val base64Signature: String? = null,
    val messageDigest: MessageDigestInfo? = null
)
private data class SigstoreBundle(val messageSignature: MessageSignature? = null)

class BundleVerifier(private val publicKey: PublicKey) {

    fun verify(artifact: File, bundleJson: String): VerificationResult {
        val bundle = Moshi.asA<SigstoreBundle>(bundleJson)
        val msg = bundle.messageSignature

        val signatureB64 = msg?.signature ?: msg?.base64Signature
            ?: return VerificationResult(artifact.name, false, "No signature found in bundle")

        val digestB64 = msg?.messageDigest?.digest
        val signatureBytes = Base64.getDecoder().decode(signatureB64)
        val artifactBytes = artifact.readBytes()

        if (digestB64 != null) {
            val digestBytes = Base64.getDecoder().decode(digestB64)
            val actualDigest = MessageDigest.getInstance("SHA-256").digest(artifactBytes)
            if (!actualDigest.contentEquals(digestBytes)) {
                return VerificationResult(artifact.name, false, "Artifact digest mismatch — file may have been tampered with")
            }
        }

        val sig = Signature.getInstance("SHA256withECDSA").apply {
            initVerify(publicKey)
            update(artifactBytes)
        }

        return when {
            sig.verify(signatureBytes) -> VerificationResult(artifact.name, true, "Verified OK")
            else -> VerificationResult(artifact.name, false, "Signature verification failed")
        }
    }

}
