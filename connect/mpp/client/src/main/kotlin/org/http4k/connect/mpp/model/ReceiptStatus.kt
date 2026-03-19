package org.http4k.connect.mpp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ReceiptStatus private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ReceiptStatus>(::ReceiptStatus) {
        val success = ReceiptStatus.of("success")
    }
}
