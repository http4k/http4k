package org.http4k.ai.llm

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class AzureRegion private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AzureRegion>(::AzureRegion)
}
