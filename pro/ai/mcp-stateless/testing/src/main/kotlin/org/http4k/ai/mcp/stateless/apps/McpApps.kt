/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.apps

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.stateless.ResourceRequest
import org.http4k.ai.mcp.stateless.ResourceResponse.Error
import org.http4k.ai.mcp.stateless.ToolRequest
import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.apps.McpServerResult.Failure
import org.http4k.ai.mcp.stateless.apps.McpServerResult.Success
import org.http4k.ai.mcp.stateless.apps.McpServerResult.Unknown
import org.http4k.ai.mcp.stateless.apps.model.AvailableMcpApp
import org.http4k.ai.mcp.stateless.apps.model.HostToolRequest
import org.http4k.ai.mcp.stateless.apps.model.HostToolResponse
import org.http4k.ai.mcp.stateless.client.McpClient
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.apps.Csp
import org.http4k.ai.mcp.stateless.model.apps.McpAppMeta
import org.http4k.ai.mcp.stateless.util.auto
import org.http4k.core.Uri
import org.http4k.lens.stateless.MetaKey

/**
 * Facade to interact with MCP apps.
 */
class McpApps(private val clients: List<McpClient>) {

    private val serverClients = mutableMapOf<String, McpClient>()

    fun start() {
        clients.forEachIndexed { index, client ->
            val serverId = client.discover().valueOrNull()?.name?.value ?: "server-$index"
            serverClients[serverId] = client
        }
    }

    fun tools() = serverClients
        .mapNotNull { (serverId, client) ->
            client.tools().list()
                .map {
                    it.mapNotNull { tool -> MetaKey.auto(McpAppMeta).toLens()(tool._meta)?.resourceUri?.let { tool.name to it } }
                        .map { AvailableMcpApp(serverId, serverId, it.first, it.second) }
                }
                .valueOrNull()
        }.flatten()

    fun callTool(request: HostToolRequest) = when (val s = findServerFor(request.serverId)) {
        null -> Unknown

        else -> s.tools().call(request.name, ToolRequest(request.arguments))
            .map {
                when (it) {
                    is Ok -> Success(HostToolResponse(it.content ?: emptyList()))
                    else -> Failure(it.toString())
                }
            }
            .recover { Failure(it.toString()) }
    }

    private fun findServerFor(serverId: String) =
        serverClients[serverId]

    fun render(serverId: String, resourceUri: Uri): McpServerResult<ResourceResponse> =
        when (val s = findServerFor(serverId)) {
            null -> Unknown

            else -> s.resources().read(ResourceRequest(resourceUri))
                .map {
                    when (it) {
                        is org.http4k.ai.mcp.stateless.ResourceResponse.Ok -> {
                            val textContents = it.list.filterIsInstance<Resource.Content.Text>()
                            Success(
                                ResourceResponse(
                                    textContents.joinToString("") { it.text.replace("\"", "'") },
                                    textContents.firstNotNullOfOrNull { it._meta?.ui?.csp }
                                )
                            )
                        }

                        is Error -> Failure(it.message)

                        is org.http4k.ai.mcp.stateless.ResourceResponse.InputRequired -> Failure("input required")
                    }
                }
                .recover { Failure(it.toString()) }
        }
}

data class ResourceResponse(val content: String, val csp: Csp?)

sealed interface McpServerResult<out T> {
    data class Success<T>(val value: T) : McpServerResult<T>
    data class Failure(val reason: String) : McpServerResult<Nothing>
    object Unknown : McpServerResult<Nothing>
}
