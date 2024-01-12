package org.http4k.webhook

import org.http4k.core.Body

fun interface WebhookSigner {
    operator fun invoke(id: WebhookId, timestamp: WebhookTimestamp, body: Body): WebhookSignature
}
