package org.http4k.routing

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.sse.RemoteMcpTransport
import org.http4k.mcp.server.capability.CapabilityPack
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.http.Http
import org.http4k.mcp.server.http.StandardHttpMcp
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.sse.Sse
import org.http4k.mcp.server.sse.StandardSseMcp
import org.http4k.mcp.server.stdio.StdIoMcpTransport
import org.http4k.mcp.server.ws.StandardWsMcp
import org.http4k.mcp.server.ws.Websocket
import org.http4k.mcp.util.Startable
import org.http4k.mcp.util.readLines
import java.io.Reader
import java.io.Writer
import java.util.UUID

/**
 * Create an SSE MCP app from a set of feature bindings.
 *
 * This is the main entry point for the MCP server. It sets up the SSE connection and then provides a
 *  endpoint for the client to send messages to.
 *
 *  The standard paths used are:
 *      /sse <-- setup the SSE connection to an MCP client
 *      /messages <-- receive commands from connected MCP clients
 */
fun mcpSse(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    StandardSseMcp(
        McpProtocol(
            RemoteMcpTransport(McpSession.Sse()).also { it.start() },
            serverMetaData,
            capabilities
        )
    )

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpWs(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    StandardWsMcp(
        McpProtocol(
            RemoteMcpTransport(McpSession.Websocket()).also { it.start() },
            serverMetaData,
            capabilities
        )
    )

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpHttp(mcpEntity: McpEntity, version: Version, vararg capabilities: ServerCapability) =
    StandardHttpMcp(
        McpProtocol(
            RemoteMcpTransport(McpSession.Http()).also { it.start() },
            ServerMetaData(mcpEntity, version),
            capabilities
        )
    )

/**
 * Create a StdIO MCP app from a set of feature bindings.
 */
fun mcpStdIo(
    serverMetaData: ServerMetaData,
    vararg capabilities: ServerCapability,
    reader: Reader = System.`in`.reader(),
    writer: Writer = System.out.writer(),
): Startable {
    val protocol = McpProtocol(StdIoMcpTransport(writer), serverMetaData, capabilities)
    return object : Startable {
        override fun start(executor: SimpleScheduler) {
            executor.readLines(reader) {
                try {
                    protocol.receive(SessionId.of(UUID(0, 0).toString()), Request(POST, "").body(it))
                } catch (e: Exception) {
                    e.printStackTrace(System.err)
                }
            }
        }
    }
}

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)

fun compose(vararg bindings: ServerCapability) = CapabilityPack(bindings = bindings)
