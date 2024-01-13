package org.http4k.webhook.signing

import org.http4k.core.Body
import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookTimestamp

fun interface WebhookSigner {
    operator fun invoke(id: WebhookId, timestamp: WebhookTimestamp, body: Body): WebhookSignature
}
