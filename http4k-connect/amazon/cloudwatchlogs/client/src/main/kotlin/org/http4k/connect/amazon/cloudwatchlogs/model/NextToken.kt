package org.http4k.connect.amazon.cloudwatchlogs.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class NextToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<NextToken>(::NextToken)
}
