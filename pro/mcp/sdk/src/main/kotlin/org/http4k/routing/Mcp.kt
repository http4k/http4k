package org.http4k.routing

import dev.forkhandles.time.executors.SimpleSchedulerService
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
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.server.capability.CapabilityPack
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.http.Http
import org.http4k.mcp.server.http.StandardHttpMcpHandler
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.sse.Sse
import org.http4k.mcp.server.sse.StandardSseMcpHandler
import org.http4k.mcp.server.stdio.StdIoMcpProtocol
import org.http4k.mcp.server.ws.StandardWsMcpHandler
import org.http4k.mcp.server.ws.Websocket
import java.io.Reader
import java.io.Writer

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
    StandardSseMcpHandler(RealtimeMcpProtocol(McpSession.Sse(), serverMetaData, capabilities).also { it.start() })

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpWs(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    StandardWsMcpHandler(RealtimeMcpProtocol(McpSession.Websocket(), serverMetaData, capabilities).also { it.start() })

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpHttp(mcpEntity: McpEntity, version: Version, vararg capabilities: ServerCapability) =
    StandardHttpMcpHandler(
        RealtimeMcpProtocol(McpSession.Http(), ServerMetaData(mcpEntity, version), capabilities)
    )

/**
 * Create a StdIO MCP app from a set of feature bindings.
 */
fun mcpStdIo(
    serverMetaData: ServerMetaData,
    vararg capabilities: ServerCapability,
    reader: Reader = System.`in`.reader(),
    writer: Writer = System.out.writer(),
) {
    StdIoMcpProtocol(
        serverMetaData,
        reader,
        writer,
        Prompts(capabilities.filterIsInstance<PromptCapability>()),
        Tools(capabilities.filterIsInstance<ToolCapability>()),
        Resources(capabilities.filterIsInstance<ResourceCapability>()),
        Completions(capabilities.filterIsInstance<CompletionCapability>()),
    ).start(SimpleSchedulerService(1))
}

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)

fun compose(vararg bindings: ServerCapability) = CapabilityPack(bindings = bindings)
