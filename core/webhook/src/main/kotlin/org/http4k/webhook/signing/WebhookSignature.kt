package org.http4k.webhook.signing

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.toValidator

class WebhookSignature private constructor(value: String) : StringValue(value) {
    val identifier get() = SignatureIdentifier.valueOf(value.split(",")[0])
    val payload get() = SignedPayload.of(value.split(",")[1])

    companion object : StringValueFactory<WebhookSignature>(::WebhookSignature, { it: String -> it.split(",").size == 2 }.toValidator()) {
        fun of(identifier: SignatureIdentifier, payload: SignedPayload) = of("$identifier,$payload")
    }
}
