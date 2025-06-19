package org.http4k.ai.llm

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class AzureResource private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AzureResource>(::AzureResource)
}
