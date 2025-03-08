package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.core.Uri
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpNodeType
import java.time.Duration
import kotlin.random.Random

internal class ClientResources(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val random: Random,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Resources {

    private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

    override fun onChange(fn: () -> Unit) {
        register(McpResource.List, McpCallback(McpResource.List.Changed.Notification::class) { fn() })
    }

    override fun subscribe(uri: Uri, fn: () -> Unit) {
        register(McpResource.Updated, McpCallback(McpResource.Updated.Notification::class) {
            subscriptions[uri]?.forEach { it() }
        })
        subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
    }

    override fun unsubscribe(uri: Uri) {
        subscriptions -= uri
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpResource.List, McpResource.List.Request(),
        overrideDefaultTimeout ?: defaultTimeout,
        RequestId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpResource.List.Response>() }
        .map { it.resources }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
        sender(McpResource.Read, McpResource.Read.Request(request.uri), overrideDefaultTimeout ?: defaultTimeout, RequestId.random(random))
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpResource.Read.Response>() }
            .map { ResourceResponse(it.contents) }
}
