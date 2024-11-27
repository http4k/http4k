package org.http4k.connect.amazon.lambda.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId

class FunctionName private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<FunctionName>(::FunctionName)
}
