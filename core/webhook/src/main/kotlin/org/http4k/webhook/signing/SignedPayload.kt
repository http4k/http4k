package org.http4k.webhook.signing

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import java.util.Base64

class SignedPayload private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SignedPayload>(::SignedPayload) {
        fun encode(unencoded: String) = encode(unencoded.toByteArray())
        fun encode(unencoded: ByteArray) = SignedPayload(Base64.getEncoder().encodeToString(unencoded))
    }
}
