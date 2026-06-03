/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.recover
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.x402.PaymentCheck.Free
import org.http4k.ai.mcp.x402.PaymentCheck.Required
import org.http4k.ai.mcp.x402.SettlementMode.SettleAfter
import org.http4k.ai.mcp.x402.SettlementMode.SettleBefore
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.filter.McpFilters
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.MetaKey

fun McpFilters.X402PaymentRequired(
    facilitator: X402Facilitator,
    mode: SettlementMode = SettleBefore,
    check: (McpRequest) -> PaymentCheck,
) = McpFilter { next ->
    { req ->
        when (val result = check(req)) {
            is Free -> next(req)
            is Required -> {
                val rawParams = McpJson.fields(McpJson.parse(req.http.bodyString())).toMap()["params"]
                val metaNode = (rawParams as? MoshiObject)?.attributes?.get("_meta") as? MoshiObject
                val meta = Meta(metaNode ?: MoshiObject())
                val id = req.message.id

                MetaKey.x402PaymentPayload().toLens()(meta)
                    ?.let { payment ->
                        result.requirements
                            .firstOrNull { it.scheme == payment.scheme && it.network == payment.network }
                            ?.let { matched ->
                                when (mode) {
                                    SettleBefore -> settleBefore(facilitator, payment, matched, next, req, id)
                                    SettleAfter -> settleAfter(facilitator, payment, matched, next, req, id)
                                }
                            } ?: McpResponse.Ok(
                            McpJsonRpcErrorResponse(id, ErrorMessage(402, "Unsupported payment scheme/network"))
                        )
                    } ?: McpResponse.Ok(McpJsonRpcErrorResponse(id, ErrorMessage(402, "Payment required")))
            }
        }
    }
}

private fun settleBefore(
    facilitator: X402Facilitator,
    payment: PaymentPayload,
    matched: PaymentRequirements,
    next: (McpRequest) -> McpResponse,
    req: McpRequest,
    id: Any?
) = facilitator(Verify(payment, matched))
    .flatMap {
        facilitator(Settle(payment, matched))
            .mapFailure {
                RemoteFailure(it.method, it.uri, it.status, "Settlement failed: ${it.message}")
            }
    }
    .map { next(req) }
    .recover {
        McpResponse.Ok(
            McpJsonRpcErrorResponse(id, ErrorMessage(402, it.message ?: "Payment failed"))
        )
    }

private fun settleAfter(
    facilitator: X402Facilitator,
    payment: PaymentPayload,
    matched: PaymentRequirements,
    next: (McpRequest) -> McpResponse,
    req: McpRequest,
    id: Any?
) = facilitator(Verify(payment, matched))
    .map { next(req) }
    .flatMap { response ->
        facilitator(Settle(payment, matched))
            .map { response }
            .mapFailure {
                RemoteFailure(
                    it.method,
                    it.uri,
                    it.status,
                    "Settlement failed: ${it.message}"
                )
            }
    }
    .recover {
        McpResponse.Ok(
            McpJsonRpcErrorResponse(id, ErrorMessage(402, it.message ?: "Payment failed"))
        )
    }
