package org.http4k.connect.amazon.cloudfront.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId
import java.util.UUID

class CallerReference private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<CallerReference>(::CallerReference) {
        fun random() = of(UUID.randomUUID().toString())
    }
}

class DistributionId private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<DistributionId>(::DistributionId)
}
