package org.http4k.ai.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.`2024-11-05`
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.testing.capabilities.TestingCompletions
import org.http4k.ai.mcp.testing.capabilities.TestingElicitations
import org.http4k.ai.mcp.testing.capabilities.TestingPrompts
import org.http4k.ai.mcp.testing.capabilities.TestingRequestProgress
import org.http4k.ai.mcp.testing.capabilities.TestingResources
import org.http4k.ai.mcp.testing.capabilities.TestingSampling
import org.http4k.ai.mcp.testing.capabilities.TestingTools

/**
 * Create an in-memory MCP test client - HTTP Streaming only. For Non-HTTP Streaming, use HttpNonStreamingMcpClient
 */
fun PolyHandler.testMcpClient(connectRequest: Request = Request(POST, "/mcp")) =
    org.http4k.ai.mcp.testing.TestMcpClient(this, connectRequest)

class TestMcpClient(poly: PolyHandler, connectRequest: Request) : McpClient {

    private val sender = org.http4k.ai.mcp.testing.TestMcpSender(poly, connectRequest)
    private val tools = org.http4k.ai.mcp.testing.capabilities.TestingTools(sender)
    private val prompts = TestingPrompts(sender)
    private val progress = TestingRequestProgress(sender)
    private val sampling = TestingSampling(sender)
    private val elicitations = TestingElicitations(sender)
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
        return initResponse.first()
            .nextEvent<ServerCapabilities, McpInitialize.Response>(fun McpInitialize.Response.(): ServerCapabilities {
                return capabilities
            })
            .map { it.second }
    }

    override fun tools() = tools

    override fun prompts() = prompts

    override fun progress() = progress

    override fun sampling() = sampling

    override fun elicitations() = elicitations

    override fun resources() = resources

    override fun completions() = completions

    override fun close() {}
}
