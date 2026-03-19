/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.x402.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class PaymentAmount private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PaymentAmount>(::PaymentAmount)
}
