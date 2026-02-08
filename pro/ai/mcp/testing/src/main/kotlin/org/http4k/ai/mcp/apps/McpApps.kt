package org.http4k.ai.mcp.apps

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
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
import org.http4k.ai.mcp.apps.model.AvailableMcpApp
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.core.Uri

/**
 * Facade to interact with MCP apps.
 */
class McpApps(private val clients: List<McpClient>) {

    private val serverClients = mutableMapOf<VersionedMcpEntity, McpClient>()

    fun start() {
        serverClients += clients.associateBy { it.start().onFailure { throw Exception(it.toString()) }.serverInfo }
    }

    fun tools() = serverClients
        .mapNotNull { (entity, client) ->
            client.tools().list()
                .map {
                    it.mapNotNull { tool -> tool._meta?.ui?.resourceUri?.let { tool.name to it } }
                        .map { AvailableMcpApp(entity.name.value, entity.name.value, it.first, it.second) }
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
        serverClients.entries.firstOrNull { it.key.name.value == serverId }?.value

    fun render(serverId: String, resourceUri: Uri) = when (val s = findServerFor(serverId)) {
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

