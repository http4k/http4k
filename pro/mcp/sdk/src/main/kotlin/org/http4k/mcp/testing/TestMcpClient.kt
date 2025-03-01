package org.http4k.mcp.testing

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.testing.capabilities.TestCompletions
import org.http4k.mcp.testing.capabilities.TestPrompts
import org.http4k.mcp.testing.capabilities.TestResources
import org.http4k.mcp.testing.capabilities.TestSampling
import org.http4k.mcp.testing.capabilities.TestTools
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import java.util.concurrent.atomic.AtomicReference

/**
 * Create an in-memory MCP test client
 */
fun PolyHandler.testMcpClient(connectRequest: Request = Request(GET, "/sse")) = TestMcpClient(this, connectRequest)

class TestMcpClient(private val poly: PolyHandler, private val connectRequest: Request) : McpClient {

    private val messageRequest = AtomicReference<Request>()

    private val sender = TestMcpSender(poly, messageRequest)

    private val client = AtomicReference<TestSseClient>()

    override fun start(): McpResult<ServerCapabilities> {
        val mcpResponse = poly.sse!!.testSseClient(connectRequest)

        client.set(mcpResponse)

        require(mcpResponse.status == OK)

        val endpointEvent = mcpResponse.received().first() as SseMessage.Event

        require(endpointEvent.event == "endpoint") { "no endpoint event" }
        messageRequest.set(Request(POST, Uri.of(endpointEvent.data)))
        sender(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(McpEntity.of("client"), Version.of("1")),
                ClientCapabilities(*ProtocolCapability.entries.toTypedArray<ProtocolCapability>()), `2024-10-07`
            )
        )
        sender(McpInitialize.Initialized, McpInitialize.Initialized.Notification)

        return client.nextEvent<McpInitialize.Response, ServerCapabilities> { capabilities }
    }

    override fun tools() = TestTools(sender, client)

    override fun prompts() = TestPrompts(sender, client)

    override fun sampling() = TestSampling(sender, client)

    override fun resources() = TestResources(sender, client)

    override fun completions() = TestCompletions(sender, client)

    override fun close() {
    }
}

