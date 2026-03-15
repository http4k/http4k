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
        val reqs = requirements(req)

        paymentSignatureLens(req)?.let { payload ->
            reqs.firstOrNull { it.scheme == payload.scheme && it.network == payload.network }
                ?.let { matched ->
                    facilitator(Verify(payload, matched))
                        .flatMap { facilitator(Settle(payload, matched)) }
                        .map { next(req).with(paymentResponseLens of it) }
                        .recover { paymentRequiredResponse(reqs, it.message ?: "Payment failed", req) }
                } ?: paymentRequiredResponse(reqs, "Unsupported payment scheme/network", req)
        } ?: paymentRequiredResponse(reqs, "Payment Required", req)
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
