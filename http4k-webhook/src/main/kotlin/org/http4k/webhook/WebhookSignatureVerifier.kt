package org.http4k.webhook

import org.http4k.core.Body

fun interface WebhookSignatureVerifier {
    operator fun invoke(id: WebhookId, timestamp: WebhookTimestamp, signature: WebhookSignature, body: Body): Boolean
}
