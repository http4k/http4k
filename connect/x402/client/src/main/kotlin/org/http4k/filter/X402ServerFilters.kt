package org.http4k.filter

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.PaymentRequired
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.ResourceInfo
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.with
import org.http4k.lens.paymentRequiredLens
import org.http4k.lens.paymentResponseLens
import org.http4k.lens.paymentSignatureLens

fun ServerFilters.X402PaymentRequired(
    facilitator: X402Facilitator,
    requirements: (Request) -> List<PaymentRequirements>
) = Filter { next ->
    { req ->
        val payload = paymentSignatureLens(req)
        val reqs = requirements(req)

        when (payload) {
            null -> paymentRequiredResponse(reqs, "Payment Required", req)
            else -> facilitator(Verify(payload, reqs.first()))
                .flatMap { facilitator(Settle(payload, reqs.first())) }
                .map { next(req).with(paymentResponseLens of it) }
                .recover { paymentRequiredResponse(reqs, it.message ?: "Payment failed", req) }
        }
    }
}

private fun paymentRequiredResponse(reqs: List<PaymentRequirements>, error: String, request: Request) =
    Response(PAYMENT_REQUIRED).with(
        paymentRequiredLens of PaymentRequired(
            x402Version = 2,
            error = error,
            accepts = reqs,
            resource = ResourceInfo(url = request.uri)
        )
    )
