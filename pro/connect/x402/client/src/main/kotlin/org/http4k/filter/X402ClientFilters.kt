/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
            PAYMENT_REQUIRED -> {
                val required = paymentRequiredLens(response)
                // echo the server-advertised resource so it matches what the server checks (its own origin-form uri)
                signer.sign(required.accepts)
                    .map { next(req.with(paymentSignatureLens of it.copy(resource = required.resource?.url?.toString() ?: it.resource))) }
                    .recover { response }
            }

            else -> response
        }
    }
}
