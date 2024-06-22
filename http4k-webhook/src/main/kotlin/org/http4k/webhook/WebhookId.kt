package org.http4k.webhook

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class WebhookId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<WebhookId>(::WebhookId)
}
