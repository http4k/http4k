package org.http4k.connect.x402.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class PaymentScheme private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PaymentScheme>(::PaymentScheme)
}
