package org.http4k.mcp.client.internal

import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpNodeType

internal class ClientResources(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val sender: McpRpcSender,
    private val register: (McpRpc, NotificationCallback<*>) -> Any
) : McpClient.Resources {
    override fun onChange(fn: () -> Unit) {
        register(McpResource.List, NotificationCallback(McpResource.List.Changed.Notification::class) { fn() })
    }

    override fun list() = sender(McpResource.List, McpResource.List.Request()) { true }
        .mapCatching(queueFor)
        .map { it.first().asAOrThrow<McpResource.List.Response>() }
        .map { it.resources }

    override fun read(request: ResourceRequest) =
        sender(McpResource.Read, McpResource.Read.Request(request.uri)) { true }
            .mapCatching(queueFor)
            .map { it.first().asAOrThrow<McpResource.Read.Response>() }
            .map { ResourceResponse(it.contents) }
}
