package org.http4k.mcp.testing.capabilities

import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.mcp.testing.nextNotification
import org.http4k.testing.TestSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class TestResources(private val sender: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
    McpClient.Resources {

    private val notifications = mutableListOf<() -> Unit>()

    override fun onChange(fn: () -> Unit) {
        notifications += fn
    }

    fun expectNotification() =
        client.nextNotification<McpResource.List.Changed.Notification>(McpResource.List.Changed)
            .also { notifications.forEach { it() } }

    override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpResource>> {
        sender(McpResource.List, McpResource.List.Request())
        return client.nextEvent<McpResource.List.Response, List<McpResource>> { resources }
    }

    override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?): McpResult<ResourceResponse> {
        sender(McpResource.Read, McpResource.Read.Request(request.uri))
        return client.nextEvent<McpResource.Read.Response, ResourceResponse> { ResourceResponse(contents) }
    }
}
