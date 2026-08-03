/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.unwrap
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

@Suppress("CyclomaticComplexMethod")
fun RoutingMcpHandler(
    discover: () -> McpDiscover.Response.Result,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    cancellations: Cancellations,
    requestStateCodec: RequestStateCodec = RequestStateCodec.None,
): McpHandler = { mcp ->
    val id = mcp.message.id?.coerce()

    // MRTR requestState integrity: an incoming state must verify (else the request is rejected before the
    // handler runs), and an outgoing state is signed. Handlers only ever see/produce plaintext state.
    fun verifyState(state: String?) = state?.let { requestStateCodec.verify(it) ?: throw McpException(InvalidParams) }
    fun signState(state: String?) = state?.let(requestStateCodec::sign)

    when (mcp.message) {
        is McpDiscover.Request -> Ok(McpDiscover.Response(discover(), id))

        is McpCompletion.Request -> Ok(
            McpCompletion.Response(completions.complete(mcp.message.params, mcp.client, mcp.http), id)
        )

        is McpPrompt.Get.Request -> {
            val params = mcp.message.params.copy(requestState = verifyState(mcp.message.params.requestState))
            val result = prompts.get(params, mcp.client, mcp.http)
            Ok(McpPrompt.Get.Response(result.copy(requestState = signState(result.requestState)), id))
        }

        is McpPrompt.List.Request -> Ok(
            McpPrompt.List.Response(
                prompts.list(mcp.message.params ?: McpPrompt.List.Request.Params(), mcp.client, mcp.http),
                id
            )
        )

        is McpResource.ListTemplates.Request -> Ok(
            McpResource.ListTemplates.Response(
                resources.listTemplates(
                    mcp.message.params ?: McpResource.ListTemplates.Request.Params(),
                    mcp.client,
                    mcp.http
                ), id
            )
        )

        is McpResource.List.Request -> Ok(
            McpResource.List.Response(
                resources.listResources(mcp.message.params ?: McpResource.List.Request.Params(), mcp.client, mcp.http),
                id
            )
        )

        is McpResource.Read.Request -> {
            val params = mcp.message.params.copy(requestState = verifyState(mcp.message.params.requestState))
            val result = resources.read(params, mcp.client, mcp.http)
            Ok(McpResource.Read.Response(result.copy(requestState = signState(result.requestState)), id))
        }

        is McpTool.Call.Request -> {
            val params = mcp.message.params.copy(requestState = verifyState(mcp.message.params.requestState))
            val result = tools.call(params, mcp.client, mcp.http)
            Ok(McpTool.Call.Response(result.copy(requestState = signState(result.requestState)), id))
        }

        is McpTool.List.Request -> Ok(
            McpTool.List.Response(
                tools.list(mcp.message.params ?: McpTool.List.Request.Params(), mcp.client, mcp.http),
                id
            )
        )

        is McpCancelled.Notification -> {
            cancellations.cancel(mcp.message.params)
            Accepted
        }

        else -> Accepted
    }
}

private fun Any.coerce(): Any? = McpJson.asJsonObject(this).unwrap()
