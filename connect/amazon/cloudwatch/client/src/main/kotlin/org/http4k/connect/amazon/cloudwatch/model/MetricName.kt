package org.http4k.connect.amazon.cloudwatch.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class MetricName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MetricName>(::MetricName)
}
