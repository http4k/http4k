package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Uri
import java.time.Duration
import kotlin.random.Random

internal class ClientResources(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val random: Random,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Resources {

    private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

    override fun onChange(fn: () -> Unit) {
        register(McpResource.List, McpCallback(McpResource.List.Changed.Notification::class) { _, _ ->
            fn()
        })
    }

    override fun subscribe(uri: Uri, fn: () -> Unit) {
        register(McpResource.Updated, McpCallback(McpResource.Updated.Notification::class) { notification, _ ->
            subscriptions[notification.uri]?.forEach { it() }
        })
        sender(
            McpResource.Subscribe,
            McpResource.Subscribe.Request(uri),
            defaultTimeout,
            McpMessageId.random(random)
        )
        subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
    }

    override fun unsubscribe(uri: Uri) {
        sender(
            McpResource.Unsubscribe,
            McpResource.Unsubscribe.Request(uri),
            defaultTimeout,
            McpMessageId.random(random)
        )
        subscriptions -= uri
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpResource.List, McpResource.List.Request(),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpResource.List.Response>() }
        .map { it.resources }

    override fun listTemplates(overrideDefaultTimeout: Duration?) = sender(
        McpResource.ListTemplates, McpResource.ListTemplates.Request(),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpResource.ListTemplates.Response>() }
        .map { it.resourceTemplates }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpResource.Read,
            McpResource.Read.Request(request.uri, request.meta),
            overrideDefaultTimeout ?: defaultTimeout,
            McpMessageId.random(random)
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpResource.Read.Response>() }
            .map { ResourceResponse(it.contents) }
}
