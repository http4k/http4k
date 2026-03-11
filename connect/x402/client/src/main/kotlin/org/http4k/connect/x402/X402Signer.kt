package org.http4k.connect.x402

import dev.forkhandles.result4k.Result
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements

fun interface X402Signer {
    fun sign(requirements: List<PaymentRequirements>): Result<PaymentPayload, String>
}
