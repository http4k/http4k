package org.http4k.connect.amazon.secretsmanager.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.ResourceId

class SecretId private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<SecretId>(::SecretId) {
        fun of(arn: ARN) = SecretId.of(arn.value)
    }
}
