package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
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
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.testing.capabilities.TestMcpClientCompletions
import org.http4k.mcp.testing.capabilities.TestMcpClientPrompts
import org.http4k.mcp.testing.capabilities.TestMcpClientRequestProgress
import org.http4k.mcp.testing.capabilities.TestMcpClientResources
import org.http4k.mcp.testing.capabilities.TestMcpClientSampling
import org.http4k.mcp.testing.capabilities.TestMcpClientTools
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import java.util.concurrent.atomic.AtomicReference

/**
 * Create an in-memory MCP test client // SSE only
 */
fun PolyHandler.testMcpSseClient(connectRequest: Request = Request(GET, "/sse")) = TestMcpClient(this, connectRequest)

class TestMcpClient(private val poly: PolyHandler, private val connectRequest: Request) : McpClient {

    private val messageRequest = AtomicReference<Request>()
    private val sender = TestMcpSender(poly, messageRequest)
    private val client = AtomicReference<TestSseClient>()
    private val tools = TestMcpClientTools(sender, client)
    private val prompts = TestMcpClientPrompts(sender, client)
    private val progress = TestMcpClientRequestProgress(client)
    private val sampling = TestMcpClientSampling(sender, client)
    private val resources = TestMcpClientResources(sender, client)
    private val completions = TestMcpClientCompletions(sender, client)

    override fun start(): McpResult<ServerCapabilities> {
        val mcpResponse = poly.testSseClient(connectRequest)

        client.set(mcpResponse)

        require(mcpResponse.status == OK)

        val endpointEvent = mcpResponse.received().first() as SseMessage.Event

        require(endpointEvent.event == "endpoint") { "no endpoint event" }
        messageRequest.set(Request(POST, Uri.of(endpointEvent.data)))
        sender(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(McpEntity.of("client"), Version.of("1")),
                ClientCapabilities(), `2024-10-07`
            )
        )
        sender(McpInitialize.Initialized, McpInitialize.Initialized.Notification)

        return client.nextEvent<McpInitialize.Response, ServerCapabilities> { capabilities }.map { it.second }
    }

    override fun tools(): TestMcpClientTools = tools

    override fun prompts(): TestMcpClientPrompts = prompts
    override fun progress(): TestMcpClientRequestProgress = progress

    override fun sampling(): TestMcpClientSampling = sampling

    override fun resources(): TestMcpClientResources = resources

    override fun completions(): TestMcpClientCompletions = completions

    override fun close() {
    }
}

