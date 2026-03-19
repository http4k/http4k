/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.x402

import dev.forkhandles.result4k.Result
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements

fun interface X402Signer {
    fun sign(requirements: List<PaymentRequirements>): Result<PaymentPayload, String>
}
