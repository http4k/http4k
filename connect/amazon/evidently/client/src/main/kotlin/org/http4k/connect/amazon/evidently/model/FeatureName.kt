package org.http4k.connect.amazon.evidently.model

import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.length
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.ResourceId

class FeatureName private constructor(value: String) : ResourceId(value) {
    companion object : ValueFactory<FeatureName, String>(
        coerceFn = ::FeatureName,
        validation = (1..127).length,
        parseFn = { it.split("/").last() }
    ) {
        fun of(arn: ARN) = arn.resourceId(FeatureName::parse)
    }
}
