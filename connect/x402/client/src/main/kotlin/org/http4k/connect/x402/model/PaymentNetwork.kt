package org.http4k.connect.x402.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class PaymentNetwork private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PaymentNetwork>(::PaymentNetwork)
}
