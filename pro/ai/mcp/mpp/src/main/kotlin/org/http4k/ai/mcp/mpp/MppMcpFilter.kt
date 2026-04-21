/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Free
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Required
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.connect.mpp.MppMoshi
import org.http4k.connect.mpp.MppVerifier
import org.http4k.connect.mpp.model.Challenge
import org.http4k.filter.McpFilters
import org.http4k.format.Json
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.lens.MetaKey

private const val PAYMENT_REQUIRED_CODE = -32042
private const val VERIFICATION_FAILED_CODE = -32043

fun McpFilters.MppPaymentRequired(
    verifier: MppVerifier,
    check: (McpRequest) -> MppPaymentCheck,
) = McpFilter { next ->
    { req ->
        when (val json = req.json) {
            is JsonRpcRequest<*> -> when (val result = check(req)) {
                is Free -> next(req)
                is Required -> {
                    val params = json.params as? MoshiObject
                    val metaNode = params?.attributes?.get("_meta") as? MoshiObject
                    val meta = Meta(metaNode ?: MoshiObject())

                    MetaKey.mppCredential().toLens()(meta)
                        ?.let { credential ->
                            verifier.verify(credential)
                                .map { next(req) }
                                .recover {
                                    McpResponse(
                                        MppErrorMessage(
                                            VERIFICATION_FAILED_CODE,
                                            it.message ?: "Verification failed",
                                            result.challenges
                                        )
                                            .toJsonRpc(json.id)
                                    )
                                }
                        } ?: McpResponse(
                        MppErrorMessage(PAYMENT_REQUIRED_CODE, "Payment required", result.challenges)
                            .toJsonRpc(json.id)
                    )
                }
            }

            is JsonRpcResult<*> -> next(req)
        }
    }
}

private class MppErrorMessage(
    code: Int,
    message: String,
    private val challenges: List<Challenge>
) : ErrorMessage(code, message) {
    @Suppress("UNCHECKED_CAST")
    override fun <NODE> data(json: Json<NODE>): NODE =
        MppMoshi.asJsonObject(mapOf("challenges" to challenges)) as NODE
}
