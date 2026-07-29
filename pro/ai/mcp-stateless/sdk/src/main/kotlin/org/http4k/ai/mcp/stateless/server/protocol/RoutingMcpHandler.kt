/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.protocol

import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.stateless.protocol.messages.HasServerInfo
import org.http4k.ai.mcp.stateless.protocol.messages.McpCancelled
import org.http4k.ai.mcp.stateless.protocol.messages.McpCompletion
import org.http4k.ai.mcp.stateless.protocol.messages.McpDiscover
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.protocol.messages.McpResource
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.stateless.server.withServerInfo
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.format.unwrap
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import kotlin.reflect.KClass

class RoutingMcpHandler(
    private val metaData: ServerMetaData,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    cancellations: Cancellations,
    private val requestStateCodec: RequestStateCodec,
    private val extensions: List<McpServerExtension> = emptyList(),
) : McpHandler {

    private val routes: Map<KClass<out McpJsonRpcRequest>, Route> = mapOf(
        route<McpDiscover.Request> { _, mcp ->
            val discoverResultFor = discoverResultFor(metaData)
            Ok(
                McpDiscover.Response(
                    discoverResultFor.copy(_meta = discoverResultFor._meta.withServerInfo(metaData.entity)),
                    mcp.id()
                )
            )
        },

        route<McpCompletion.Request> { msg, mcp ->
            val complete = completions.complete(msg.params, mcp.client, mcp.http)
            Ok(McpCompletion.Response(complete.copy(_meta = complete._meta.withServerInfo(metaData.entity)), mcp.id()))
        },

        route<McpPrompt.Get.Request> { msg, mcp ->
            val result = prompts.get(msg.params.copy(requestState = msg.params.requestState.verify()), mcp.client, mcp.http)
            val copy = result.copy(requestState = result.requestState.signed())
            Ok(McpPrompt.Get.Response(copy.copy(_meta = copy._meta.withServerInfo(metaData.entity)), mcp.id()))
        },

        route<McpPrompt.List.Request> { msg, mcp ->
            val params = msg.params ?: McpPrompt.List.Request.Params()
            val list = prompts.list(params, mcp.client, mcp.http)
            Ok(McpPrompt.List.Response(list.copy(_meta = list._meta.withServerInfo(metaData.entity)), mcp.id()))
        },

        route<McpResource.ListTemplates.Request> { msg, mcp ->
            val params = msg.params ?: McpResource.ListTemplates.Request.Params()
            val listTemplates = resources.listTemplates(params, mcp.client, mcp.http)
            Ok(
                McpResource.ListTemplates.Response(
                    listTemplates.copy(_meta = listTemplates._meta.withServerInfo(metaData.entity)), mcp.id()
                )
            )
        },

        route<McpResource.List.Request> { msg, mcp ->
            val params = msg.params ?: McpResource.List.Request.Params()
            val listResources = resources.listResources(params, mcp.client, mcp.http)
            Ok(
                McpResource.List.Response(
                    listResources.copy(_meta = listResources._meta.withServerInfo(metaData.entity)),
                    mcp.id()
                )
            )
        },

        route<McpResource.Read.Request> { msg, mcp ->
            val result = resources.read(msg.params.copy(requestState = msg.params.requestState.verify()), mcp.client, mcp.http)
            val copy = result.copy(requestState = result.requestState.signed())
            Ok(McpResource.Read.Response(copy.copy(_meta = copy._meta.withServerInfo(metaData.entity)), mcp.id()))
        },

        route<McpTool.Call.Request> { msg, mcp ->
            val result = tools.call(msg.params.copy(requestState = msg.params.requestState.verify()), mcp.client, mcp.http)
            val copy = result.copy(requestState = result.requestState.signed())
            Ok(McpTool.Call.Response(copy.copy(_meta = copy._meta.withServerInfo(metaData.entity)), mcp.id()))
        },

        route<McpTool.List.Request> { msg, mcp ->
            val params = msg.params ?: McpTool.List.Request.Params()
            val list = tools.list(params, mcp.client, mcp.http)
            Ok(McpTool.List.Response(list.copy(_meta = list._meta.withServerInfo(metaData.entity)), mcp.id()))
        },

        route<McpCancelled.Notification> { msg, _ ->
            cancellations.cancel(msg.params)
            Accepted
        },
    )

    override fun invoke(mcp: McpRequest) = routes[mcp.message::class]?.handle?.invoke(mcp)
        ?: extensions.firstOrNull { mcp.message.method in it.methods }?.invoke(mcp)?.withServerInfo(metaData.entity)
        ?: Accepted

    private fun String?.verify() = this?.let { requestStateCodec.verify(it) ?: throw McpException(InvalidParams) }
    private fun String?.signed() = this?.let(requestStateCodec::sign)
}

private class Route(val handle: (McpRequest) -> McpResponse)

private inline fun <reified T : McpJsonRpcRequest> route(noinline fn: (T, McpRequest) -> McpResponse) =
    T::class to Route { mcp -> fn(mcp.message as T, mcp) }
private fun McpRequest.id() = message.id?.coerce()

private fun McpResponse.withServerInfo(info: VersionedMcpEntity) = when (this) {
    is Ok -> (message as? HasServerInfo)?.let { Ok(it.withServerInfo(info)) } ?: this
    else -> this
}

private fun Any.coerce(): Any? = McpJson.asJsonObject(this).unwrap()
