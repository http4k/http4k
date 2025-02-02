package org.http4k.mcp.client.internal

import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.sse.SseMessage

internal class ClientResources(
    private val queueFor: (RequestId) -> Iterable<SseMessage.Event>,
    private val sender: McpRpcSender,
    private val register: (NotificationCallback<*>) -> Any
) : McpClient.Resources {
    override fun onChange(fn: () -> Unit) {
        register(
            NotificationCallback(
                McpResource.List.Changed,
                McpResource.List.Changed.Notification::class
            ) { fn() }
        )
    }

    override fun list() = sender(McpResource.List, McpResource.List.Request()) { true }
        .mapCatching(queueFor).map { it.first().asAOrThrow<McpResource.List.Response>() }
        .map { it.resources }

    override fun read(name: String, request: ResourceRequest) =
        sender(McpResource.Read, McpResource.Read.Request(request.uri)) { true }
            .mapCatching(queueFor).map { it.first().asAOrThrow<McpResource.Read.Response>() }
            .map { ResourceResponse(it.contents) }
}
