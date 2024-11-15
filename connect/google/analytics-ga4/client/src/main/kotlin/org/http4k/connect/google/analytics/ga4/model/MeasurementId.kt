package org.http4k.connect.google.analytics.ga4.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class MeasurementId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MeasurementId>(::MeasurementId)
}
