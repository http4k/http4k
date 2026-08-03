/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.withServerInfo
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.unwrap
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

@Suppress("CyclomaticComplexMethod")
fun RoutingMcpHandler(
    metaData: ServerMetaData,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    cancellations: Cancellations,
    requestStateCodec: RequestStateCodec,
): McpHandler = { mcp ->
    val id = mcp.message.id?.coerce()
    val serverInfo = metaData.entity

    fun String?.verified() = this?.let { requestStateCodec.verify(it) ?: throw McpException(InvalidParams) }
    fun String?.signed() = this?.let(requestStateCodec::sign)

    when (mcp.message) {
        is McpDiscover.Request -> {
            val result = discoverResultFor(metaData)
            Ok(McpDiscover.Response(result.copy(_meta = result._meta.withServerInfo(serverInfo)), id))
        }

        is McpCompletion.Request -> {
            val result = completions.complete(mcp.message.params, mcp.client, mcp.http)
            Ok(McpCompletion.Response(result.copy(_meta = result._meta.withServerInfo(serverInfo)), id))
        }

        is McpPrompt.Get.Request -> {
            val params = mcp.message.params.copy(requestState = mcp.message.params.requestState.verified())
            val result = prompts.get(params, mcp.client, mcp.http)
            Ok(McpPrompt.Get.Response(
                result.copy(requestState = result.requestState.signed(), _meta = result._meta.withServerInfo(serverInfo)),
                id
            ))
        }

        is McpPrompt.List.Request -> {
            val result = prompts.list(mcp.message.params ?: McpPrompt.List.Request.Params(), mcp.client, mcp.http)
            Ok(McpPrompt.List.Response(result.copy(_meta = result._meta.withServerInfo(serverInfo)), id))
        }

        is McpResource.ListTemplates.Request -> {
            val result = resources.listTemplates(
                mcp.message.params ?: McpResource.ListTemplates.Request.Params(), mcp.client, mcp.http
            )
            Ok(McpResource.ListTemplates.Response(result.copy(_meta = result._meta.withServerInfo(serverInfo)), id))
        }

        is McpResource.List.Request -> {
            val result = resources.listResources(mcp.message.params ?: McpResource.List.Request.Params(), mcp.client, mcp.http)
            Ok(McpResource.List.Response(result.copy(_meta = result._meta.withServerInfo(serverInfo)), id))
        }

        is McpResource.Read.Request -> {
            val params = mcp.message.params.copy(requestState = mcp.message.params.requestState.verified())
            val result = resources.read(params, mcp.client, mcp.http)
            Ok(McpResource.Read.Response(
                result.copy(requestState = result.requestState.signed(), _meta = result._meta.withServerInfo(serverInfo)),
                id
            ))
        }

        is McpTool.Call.Request -> {
            val params = mcp.message.params.copy(requestState = mcp.message.params.requestState.verified())
            val result = tools.call(params, mcp.client, mcp.http)
            Ok(McpTool.Call.Response(
                result.copy(requestState = result.requestState.signed(), _meta = result._meta.withServerInfo(serverInfo)),
                id
            ))
        }

        is McpTool.List.Request -> {
            val result = tools.list(mcp.message.params ?: McpTool.List.Request.Params(), mcp.client, mcp.http)
            Ok(McpTool.List.Response(result.copy(_meta = result._meta.withServerInfo(serverInfo)), id))
        }

        is McpCancelled.Notification -> {
            cancellations.cancel(mcp.message.params)
            Accepted
        }

        else -> Accepted
    }
}

private fun Any.coerce(): Any? = McpJson.asJsonObject(this).unwrap()
