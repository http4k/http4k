/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.minValue

class TransactionId private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<TransactionId>(::TransactionId, 0L.minValue)
}
