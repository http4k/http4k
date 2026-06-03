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
import org.http4k.ai.mcp.x402.SettlementMode.SettleAfter
import org.http4k.ai.mcp.x402.SettlementMode.SettleBefore
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequired
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.lens.MetaKey

private val paymentLens = MetaKey.x402PaymentPayload().toLens()
private val settlementLens = MetaKey.x402Settled().toLens()

fun X402ToolFilter(
    facilitator: X402Facilitator,
    mode: SettlementMode = SettleBefore,
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
                            when (mode) {
                                SettleBefore -> settleBefore(facilitator, payment, matched, next, request, ::paymentRequiredError)
                                SettleAfter -> settleAfter(facilitator, payment, matched, next, request, ::paymentRequiredError)
                            }
                        } ?: paymentRequiredError("Unsupported payment scheme/network")
                } ?: paymentRequiredError("Payment required")
            }
        }
    }
}

private fun settleBefore(
    facilitator: X402Facilitator,
    payment: PaymentPayload,
    matched: PaymentRequirements,
    next: (ToolRequest) -> org.http4k.ai.mcp.ToolResponse,
    request: ToolRequest,
    paymentRequiredError: (String) -> Error
) = facilitator(Verify(payment, matched))
    .flatMap { facilitator(Settle(payment, matched)) }
    .map { settled ->
        when (val response = next(request)) {
            is Ok -> response.copy(meta = settlementLens(settled, response.meta))
            else -> response
        }
    }
    .recover { paymentRequiredError(it.message ?: "Payment failed") }

private fun settleAfter(
    facilitator: X402Facilitator,
    payment: PaymentPayload,
    matched: PaymentRequirements,
    next: (ToolRequest) -> org.http4k.ai.mcp.ToolResponse,
    request: ToolRequest,
    paymentRequiredError: (String) -> Error
) = facilitator(Verify(payment, matched))
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
