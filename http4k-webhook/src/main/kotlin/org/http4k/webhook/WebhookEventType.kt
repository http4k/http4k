package org.http4k.webhook

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class WebhookEventType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<WebhookEventType>(::WebhookEventType)
}
