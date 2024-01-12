package org.http4k.webhook

import org.http4k.lens.Header
import org.http4k.lens.value

val Header.WEBHOOK_ID get() = Header.value(WebhookId).required("webhook-id")
val Header.WEBHOOK_TIMESTAMP get() = Header.value(WebhookTimestamp).required("webhook-timestamp")
val Header.WEBHOOK_SIGNATURE get() = Header.value(WebhookSignature).required("webhook-signature")
