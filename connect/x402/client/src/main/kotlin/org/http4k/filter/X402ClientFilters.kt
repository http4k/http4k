package org.http4k.filter

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.x402.X402Signer
import org.http4k.core.Filter
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.with
import org.http4k.lens.paymentRequiredLens
import org.http4k.lens.paymentSignatureLens

fun ClientFilters.X402PaymentRequired(signer: X402Signer) = Filter { next ->
    { req ->
        val response = next(req)
        when (response.status) {
            PAYMENT_REQUIRED -> signer.sign(paymentRequiredLens(response).accepts)
                .map { next(req.with(paymentSignatureLens of it)) }
                .recover { response }

            else -> response
        }
    }
}
