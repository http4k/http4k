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
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.x402.PaymentCheck.Free
import org.http4k.ai.mcp.x402.PaymentCheck.Required
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Verify
import org.http4k.filter.McpFilters
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.lens.MetaKey

fun McpFilters.X402PaymentRequired(
    facilitator: X402Facilitator,
    check: (McpRequest) -> PaymentCheck,
) = McpFilter { next ->
    { req ->
        val json = req.json
        when (json) {
            is JsonRpcRequest<*> -> when (val result = check(req)) {
                is Free -> next(req)
                is Required -> {
                    val params = json.params as? MoshiObject
                    val metaNode = params?.attributes?.get("_meta") as? MoshiObject
                    val meta = Meta(metaNode ?: MoshiObject())

                    MetaKey.x402PaymentPayload().toLens()(meta)
                        ?.let { payment ->
                            result.requirements
                                .firstOrNull { it.scheme == payment.scheme && it.network == payment.network }
                                ?.let { matched ->
                                    facilitator(Verify(payment, matched))
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
                                                ErrorMessage(402, it.message ?: "Payment failed").toJsonRpc(json.id)
                                            )
                                        }
                                } ?: McpResponse.Ok(
                                ErrorMessage(402, "Unsupported payment scheme/network").toJsonRpc(json.id)
                            )
                        } ?: McpResponse.Ok(ErrorMessage(402, "Payment required").toJsonRpc(json.id))
                }
            }

            is JsonRpcResult<*> -> next(req)
        }
    }
}
