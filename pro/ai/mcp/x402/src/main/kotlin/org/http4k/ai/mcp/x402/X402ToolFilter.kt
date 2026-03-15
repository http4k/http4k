/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.ai.mcp.ToolFilter
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.x402.PaymentCheck.Free
import org.http4k.ai.mcp.x402.PaymentCheck.Required
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.PaymentRequired
import org.http4k.lens.MetaKey

private val paymentLens = MetaKey.x402PaymentPayload().toLens()
private val settlementLens = MetaKey.x402Settled().toLens()

fun X402ToolFilter(
    facilitator: X402Facilitator,
    check: (ToolRequest) -> PaymentCheck
) = ToolFilter { next ->
    { request ->
        when (val result = check(request)) {
            is Free -> next(request)
            is Required -> {
                fun paymentRequiredError(message: String) = Error(
                    content = listOf(Text(X402Moshi.asFormatString(PaymentRequired(2, message, result.requirements)))),
                    structuredContent = X402Moshi.asJsonObject(PaymentRequired(2, message, result.requirements))
                )

                paymentLens(request.meta)?.let { payment ->
                    result.requirements.firstOrNull { it.scheme == payment.scheme && it.network == payment.network }
                        ?.let { matched ->
                            facilitator(Verify(payment, matched))
                                .map { next(request) }
                                .flatMap { response ->
                                    facilitator(Settle(payment, matched))
                                        .map { settled ->
                                            when (response) {
                                                is Ok -> response.copy(meta = settlementLens(settled, response.meta))
                                                else -> response
                                            }
                                        }
                                }
                                .recover { paymentRequiredError(it.message ?: "Payment failed") }
                        } ?: paymentRequiredError("Unsupported payment scheme/network")
                } ?: paymentRequiredError("Payment required")
            }
        }
    }
}
