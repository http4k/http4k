package org.http4k.connect.openfga.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

class ContinuationToken private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ContinuationToken>(::ContinuationToken)
}
