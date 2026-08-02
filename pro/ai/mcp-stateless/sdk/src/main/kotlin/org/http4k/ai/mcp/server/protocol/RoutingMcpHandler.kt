/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.unwrap
import org.http4k.lens.Header
import org.http4k.lens.MCP_NAME

@Suppress("CyclomaticComplexMethod")
fun RoutingMcpHandler(
    discover: () -> McpDiscover.Response.Result,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    cancellations: Cancellations,
): McpHandler {
    fun McpRequest.validateMcpName(bodyName: String) = when (Header.MCP_NAME(http)) {
        null, bodyName -> null
        else -> throw McpException(HeaderMismatchError("Mcp-Name header value does not match body value"))
    }

    return ValidateMcpMethodHeader().then { mcp ->
        val id = mcp.message.id?.coerce()
        when (mcp.message) {
            is McpDiscover.Request -> McpResponse.Ok(McpDiscover.Response(discover(), id))

            is McpCompletion.Request -> McpResponse.Ok(
                McpCompletion.Response(completions.complete(mcp.message.params, mcp.client, mcp.http), id)
            )

            is McpPrompt.Get.Request -> mcp.validateMcpName(mcp.message.params.name.value) ?: McpResponse.Ok(
                McpPrompt.Get.Response(prompts.get(mcp.message.params, mcp.client, mcp.http), id)
            )

            is McpPrompt.List.Request -> McpResponse.Ok(
                McpPrompt.List.Response(
                    prompts.list(mcp.message.params ?: McpPrompt.List.Request.Params(), mcp.client, mcp.http),
                    id
                )
            )

            is McpResource.ListTemplates.Request -> McpResponse.Ok(
                McpResource.ListTemplates.Response(
                    resources.listTemplates(
                        mcp.message.params ?: McpResource.ListTemplates.Request.Params(),
                        mcp.client,
                        mcp.http
                    ), id
                )
            )

            is McpResource.List.Request -> McpResponse.Ok(
                McpResource.List.Response(
                    resources.listResources(mcp.message.params ?: McpResource.List.Request.Params(), mcp.client, mcp.http), id
                )
            )

            is McpResource.Read.Request -> mcp.validateMcpName(mcp.message.params.uri.toString()) ?: McpResponse.Ok(
                McpResource.Read.Response(resources.read(mcp.message.params, mcp.client, mcp.http), id)
            )

            is McpTool.Call.Request -> mcp.validateMcpName(mcp.message.params.name.value) ?: McpResponse.Ok(
                McpTool.Call.Response(tools.call(mcp.message.params, mcp.client, mcp.http), id)
            )

            is McpTool.List.Request -> McpResponse.Ok(
                McpTool.List.Response(
                    tools.list(mcp.message.params ?: McpTool.List.Request.Params(), mcp.client, mcp.http),
                    id
                )
            )

            is McpCancelled.Notification -> {
                cancellations.cancel(mcp.message.params)
                McpResponse.Accepted
            }

            else -> McpResponse.Accepted
        }
    }
}

private fun Any.coerce(): Any? = McpJson.asJsonObject(this).unwrap()
