/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpSubscriptions
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.unwrap
import org.http4k.lens.Header
import org.http4k.lens.MCP_NAME

// ponytail: stateless — no initializer, no sessions, no clientTracking. Capability handlers get
// Client.NoOp (server->client is MRTR, Stage 4). Mcp-Name is validated per-request off the header.
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
        when (mcp.message) {
            is McpDiscover.Request ->
                McpResponse.Ok(McpDiscover.Response(discover(), mcp.message.id?.coerce()))

            is McpCompletion.Request -> McpResponse.Ok(
                McpCompletion.Response(completions.complete(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce())
            )

            is McpPrompt.Get.Request -> mcp.validateMcpName(mcp.message.params.name.value) ?: McpResponse.Ok(
                McpPrompt.Get.Response(prompts.get(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce())
            )

            is McpPrompt.List.Request -> McpResponse.Ok(
                McpPrompt.List.Response(
                    prompts.list(mcp.message.params ?: McpPrompt.List.Request.Params(), NoOp, mcp.http),
                    mcp.message.id?.coerce()
                )
            )

            is McpResource.ListTemplates.Request -> McpResponse.Ok(
                McpResource.ListTemplates.Response(
                    resources.listTemplates(mcp.message.params ?: McpResource.ListTemplates.Request.Params(), NoOp, mcp.http),
                    mcp.message.id?.coerce()
                )
            )

            is McpResource.List.Request -> McpResponse.Ok(
                McpResource.List.Response(
                    resources.listResources(mcp.message.params ?: McpResource.List.Request.Params(), NoOp, mcp.http),
                    mcp.message.id?.coerce()
                )
            )

            is McpResource.Read.Request -> mcp.validateMcpName(mcp.message.params.uri.toString()) ?: McpResponse.Ok(
                McpResource.Read.Response(resources.read(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce())
            )

            is McpTool.Call.Request -> mcp.validateMcpName(mcp.message.params.name.value) ?: McpResponse.Ok(
                McpTool.Call.Response(tools.call(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce())
            )

            is McpTool.List.Request -> McpResponse.Ok(
                McpTool.List.Response(
                    tools.list(mcp.message.params ?: McpTool.List.Request.Params(), NoOp, mcp.http),
                    mcp.message.id?.coerce()
                )
            )

            is McpProgress.Notification -> McpResponse.Accepted

            is McpCancelled.Notification -> {
                cancellations.cancel(mcp.message.params)
                McpResponse.Accepted
            }

            is McpPrompt.List.Changed.Notification -> McpResponse.Accepted
            is McpTool.List.Changed.Notification -> McpResponse.Accepted
            is McpResource.List.Changed.Notification -> McpResponse.Accepted
            is McpResource.Updated.Notification -> McpResponse.Accepted
            is McpLogging.LoggingMessage.Notification -> McpResponse.Accepted

            // subscriptions/listen is served on the SSE path (Increment 2); reaching here via plain POST is a no-op.
            is McpSubscriptions.Listen.Request -> McpResponse.Accepted
            is McpSubscriptions.Acknowledged.Notification -> McpResponse.Accepted
        }
    }
}

private fun Any.coerce(): Any? = McpJson.asJsonObject(this).unwrap()
