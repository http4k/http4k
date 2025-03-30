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

/**
 * Create an in-memory MCP test client - HTTP Streaming only
 */
fun PolyHandler.testMcpClient(connectRequest: Request = Request(POST, "/mcp")) = TestMcpClient(this, connectRequest)

class TestMcpClient(private val poly: PolyHandler, private val connectRequest: Request) : McpClient {

    private val send = TestMcpSender(poly, connectRequest)
    private val tools = TestingTools(send)
    private val prompts = TestingPrompts(send)
//    private val progress = TestMcpClientRequestProgress(client)
//    private val sampling = TestMcpClientSampling(send, client)
    private val resources = TestingResources(send)
    private val completions = TestingCompletions(send)


    override fun start(): McpResult<ServerCapabilities> {
        val initResponse = send(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(McpEntity.of("client"), Version.of("1")),
                ClientCapabilities(), `2024-11-05`
            )
        )

        send(McpInitialize.Initialized, McpInitialize.Initialized.Notification).received().toList()
        return initResponse.nextEvent<McpInitialize.Response, ServerCapabilities> { capabilities }.map { it.second }
    }

    override fun tools() = tools

    override fun prompts() = prompts

    override fun progress() = TODO()

    override fun sampling() = TODO()

    override fun resources() = resources

    override fun completions() = completions

    override fun close() {
    }
}

