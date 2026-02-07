package org.http4k.ai.mcp.apps

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.apps.McpServerResult.Failure
import org.http4k.ai.mcp.apps.McpServerResult.Success
import org.http4k.ai.mcp.apps.McpServerResult.Unknown
import org.http4k.ai.mcp.apps.model.HostToolRequest
import org.http4k.ai.mcp.apps.model.HostToolResponse
import org.http4k.ai.mcp.apps.model.ToolOption
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.protocol.Version
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

class ConnectedMcpServers(servers: List<Uri>, http: HttpHandler) {
    private val serverClients = servers.associateWith {
        HttpStreamingMcpClient(
            McpEntity.of("http4k MCP Testing"),
            Version.of("0.0.0"),
            it, http
        )
    }

    fun start() = serverClients.values.forEach { it.start() }

    fun tools() = serverClients
        .mapNotNull { (serverUri, client) ->
            client.tools().list()
                .map {
                    it.mapNotNull { tool -> tool._meta?.ui?.resourceUri?.let { tool.name to it } }
                        .map { ToolOption(serverUri, serverUri.host, it.first, it.second) }
                }
                .valueOrNull()
        }.flatten()

    fun callTool(request: HostToolRequest): McpServerResult<HostToolResponse> =
        when (val s = serverClients[request.serverId]) {
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

    fun render(serverId: Uri, resourceUri: Uri) = when (val s = serverClients[serverId]) {
        null -> Unknown
        else -> s.resources().read(ResourceRequest(resourceUri))
            .map {
                Success(
                    it.list.filterIsInstance<Resource.Content.Text>()
                        .joinToString("") { it.text.replace("\"", "'") }
                )
            }
            .recover { Failure(it.toString()) }
    }
}

sealed interface McpServerResult<out T> {
    data class Success<T>(val value: T) : McpServerResult<T>
    data class Failure(val reason: String) : McpServerResult<Nothing>
    object Unknown : McpServerResult<Nothing>
}

