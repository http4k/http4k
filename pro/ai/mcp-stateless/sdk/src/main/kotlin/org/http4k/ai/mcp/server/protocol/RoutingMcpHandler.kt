/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

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

@Suppress("CyclomaticComplexMethod")
fun RoutingMcpHandler(
    discover: () -> McpDiscover.Response.Result,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    cancellations: Cancellations,
): McpHandler = { mcp ->
    val id = mcp.message.id?.coerce()
    when (mcp.message) {
        is McpDiscover.Request -> Ok(McpDiscover.Response(discover(), id))

        is McpCompletion.Request -> Ok(
            McpCompletion.Response(completions.complete(mcp.message.params, mcp.client, mcp.http), id)
        )

        is McpPrompt.Get.Request -> Ok(
            McpPrompt.Get.Response(prompts.get(mcp.message.params, mcp.client, mcp.http), id)
        )

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

        is McpResource.Read.Request -> Ok(
            McpResource.Read.Response(resources.read(mcp.message.params, mcp.client, mcp.http), id)
        )

        is McpTool.Call.Request -> Ok(
            McpTool.Call.Response(tools.call(mcp.message.params, mcp.client, mcp.http), id)
        )

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
