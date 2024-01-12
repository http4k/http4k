package org.http4k.webhook

import org.http4k.core.Body
import org.http4k.security.HmacSha256.hmacSHA256
import org.http4k.webhook.SignatureIdentifier.v1

/**
 * HMAC 256 symmetric implementation of Webhook signing scheme
 */
class HmacSha256(secretKey: () -> String) {
    val Signer = WebhookSigner { id, timestamp, body ->
        webhookSignatureFor(id, timestamp, body, secretKey)
    }
    val Verifier = WebhookSignatureVerifier { id, timestamp, signature, body ->
        signature == webhookSignatureFor(id, timestamp, body, secretKey)
    }

    private fun webhookSignatureFor(
        id: WebhookId,
        timestamp: WebhookTimestamp,
        body: Body,
        secretKey: () -> String
    ): WebhookSignature {
        val contentToSign = "$id.${timestamp.asInstant()}.${String(body.payload.array())}"
        return WebhookSignature.of(v1, SignedPayload.encode(hmacSHA256(secretKey().toByteArray(), contentToSign)))
    }

}
