package org.http4k.ai.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ReasoningEffort private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ReasoningEffort>(::ReasoningEffort) {
        val NONE = ReasoningEffort.of("none")
        val LOW = ReasoningEffort.of("low")
        val MEDIUM = ReasoningEffort.of("medium")
        val HIGH = ReasoningEffort.of("high")
    }
}
