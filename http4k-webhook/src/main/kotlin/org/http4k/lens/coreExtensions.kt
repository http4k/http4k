package org.http4k.lens

import org.http4k.webhook.WebhookId
import org.http4k.webhook.WebhookTimestamp
import org.http4k.webhook.signing.WebhookSignature

val Header.WEBHOOK_ID get() = Header.value(WebhookId).required("webhook-id")
val Header.WEBHOOK_TIMESTAMP get() = Header.value(WebhookTimestamp).required("webhook-timestamp")
val Header.WEBHOOK_SIGNATURE get() = Header.value(WebhookSignature).required("webhook-signature")
