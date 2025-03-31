package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-11-05`
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.testing.capabilities.TestingSampling
import org.http4k.mcp.testing.capabilities.TestingCompletions
import org.http4k.mcp.testing.capabilities.TestingPrompts
import org.http4k.mcp.testing.capabilities.TestingRequestProgress
import org.http4k.mcp.testing.capabilities.TestingResources
import org.http4k.mcp.testing.capabilities.TestingTools

/**
 * Create an in-memory MCP test client - HTTP Streaming only. For Non-HTTP Streaming, use HttpNonStreamingMcpClient
 */
fun PolyHandler.testMcpClient(connectRequest: Request = Request(POST, "/mcp")) = TestMcpClient(this, connectRequest)

class TestMcpClient(poly: PolyHandler, connectRequest: Request) : McpClient {

    private val sender = TestMcpSender(poly, connectRequest)
    private val tools = TestingTools(sender)
    private val prompts = TestingPrompts(sender)
    private val progress = TestingRequestProgress(sender)
    private val sampling = TestingSampling(sender)
    private val resources = TestingResources(sender)
    private val completions = TestingCompletions(sender)

    override fun start(): McpResult<ServerCapabilities> {
        val initResponse = sender(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(McpEntity.of("client"), Version.of("1")),
                ClientCapabilities(), `2024-11-05`
            )
        )

        sender(McpInitialize.Initialized, McpInitialize.Initialized.Notification).toList()
        return initResponse.nextEvent<McpInitialize.Response, ServerCapabilities> { capabilities }.map { it.second }
    }

    override fun tools() = tools

    override fun prompts() = prompts

    override fun progress() = progress

    override fun sampling() = sampling

    override fun resources() = resources

    override fun completions() = completions

    override fun close() {}
}
