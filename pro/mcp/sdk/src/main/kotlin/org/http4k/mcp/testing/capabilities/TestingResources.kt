package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.map
import org.http4k.core.Uri
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.mcp.testing.nextNotification
import org.http4k.sse.SseMessage
import java.time.Duration

class TestingResources(
    private val sender: TestMcpSender
) : McpClient.Resources {
    private val changeNotifications = mutableListOf<() -> Unit>()

    private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

    override fun onChange(fn: () -> Unit) {
        changeNotifications += fn
    }

    /**
     * Expect a resource list notification to be made and process it
     */
    fun expectNotification() =
        sender.stream().nextNotification<McpResource.List.Changed.Notification>(McpResource.List.Changed)
            .also { changeNotifications.forEach { it() } }

    /**
     * Expect a resource updated notification to be made and process it
     */
    fun expectSubscriptionNotification(uri: Uri) =
        sender.stream().nextNotification<McpResource.Updated.Notification>(McpResource.Updated)
            .also {
                require(it.uri == uri) { "Expected notification for $uri, but got ${it.uri}" }
                subscriptions[it.uri]?.forEach { it() }
            }

    override fun list(overrideDefaultTimeout: Duration?) =
        sender(
            McpResource.List,
            McpResource.List.Request()
        ).nextEvent<McpResource.List.Response, List<McpResource>> { resources }.map { it.second }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpResource.Read,
            McpResource.Read.Request(request.uri, request.meta)
        ).nextEvent<McpResource.Read.Response, ResourceResponse> { ResourceResponse(contents) }
            .map { it.second }


    override fun subscribe(uri: Uri, fn: () -> Unit) {
        sender(McpResource.Subscribe, McpResource.Subscribe.Request(uri))
        subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
    }

    override fun unsubscribe(uri: Uri) {
        sender(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(uri))
        subscriptions -= uri
    }
}
