package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonBlankStringValueFactory

class KMSKeyId private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<KMSKeyId>(::KMSKeyId) {
        fun of(arn: ARN) = of(arn.value)
    }
}
