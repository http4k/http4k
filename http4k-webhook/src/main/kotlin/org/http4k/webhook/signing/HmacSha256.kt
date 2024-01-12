package org.http4k.webhook.signing

import org.http4k.core.Body
import org.http4k.security.HmacSha256.hmacSHA256
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookTimestamp
import org.http4k.webhook.signing.SignatureIdentifier.v1

/**
 * HMAC 256 symmetric implementation of Webhook signing scheme
 */
object HmacSha256 {

    fun Signer(signingSecret: HmacSha256SigningSecret) = WebhookSigner { id, timestamp, body ->
        calculateSignature(id, timestamp, body, signingSecret)
    }

    fun Verifier(signingSecret: HmacSha256SigningSecret) = WebhookSignatureVerifier { id, timestamp, signature, body ->
        signature == calculateSignature(id, timestamp, body, signingSecret)
    }

    private fun calculateSignature(
        id: WebhookId,
        timestamp: WebhookTimestamp,
        body: Body,
        secret: HmacSha256SigningSecret
    ): WebhookSignature {
        val contentToSign = "$id.${timestamp.asInstant()}.${String(body.payload.array())}"
        return WebhookSignature.of(
            v1,
            SignedPayload.encode(hmacSHA256(secret.withNoPrefix().toByteArray(), contentToSign))
        )
    }
}
