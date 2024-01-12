package org.http4k.webhook

data class WebhookPayload<T : Any>(
    val type: WebhookEventType,
    val timestamp: WebhookTimestamp,
    val data: T
)
