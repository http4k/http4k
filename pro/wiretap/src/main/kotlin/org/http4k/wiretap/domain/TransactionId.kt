package org.http4k.wiretap.domain

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.minValue

class TransactionId private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<TransactionId>(::TransactionId, 0L.minValue)
}
