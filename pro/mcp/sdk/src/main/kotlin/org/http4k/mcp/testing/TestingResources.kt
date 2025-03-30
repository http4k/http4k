package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.core.Uri
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.protocol.messages.McpResource
import java.time.Duration

class TestingResources(private val send: TestMcpSender) : McpClient.Resources {

    override fun onChange(fn: () -> Unit) {
    }

    override fun list(overrideDefaultTimeout: Duration?) =
        send(
            McpResource.List,
            McpResource.List.Request()
        ).nextEvent<McpResource.List.Response, List<McpResource>> { resources }.map { it.second }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
        send(
            McpResource.Read,
            McpResource.Read.Request(request.uri)
        ).nextEvent<McpResource.Read.Response, ResourceResponse> { ResourceResponse(contents) }
            .map { it.second }


    override fun subscribe(uri: Uri, fn: () -> Unit) {
        send(McpResource.Subscribe, McpResource.Subscribe.Request(uri))
//            subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
    }

    override fun unsubscribe(uri: Uri) {
        send(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(uri))
//            subscriptions -= uri
    }
}
