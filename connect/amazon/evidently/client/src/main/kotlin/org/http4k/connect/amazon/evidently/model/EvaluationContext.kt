package org.http4k.connect.amazon.evidently.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class EvaluationContext private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<EvaluationContext>(::EvaluationContext)
}
