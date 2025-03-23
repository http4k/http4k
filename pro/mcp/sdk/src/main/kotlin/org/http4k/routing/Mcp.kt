package org.http4k.routing

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.capability.CapabilityPack
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.ServerPrompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.http.HttpNonStreamingMcp
import org.http4k.mcp.server.http.HttpStreamingMcp
import org.http4k.mcp.server.http.HttpStreamingSessions
import org.http4k.mcp.server.jsonrpc.JsonRpcMcp
import org.http4k.mcp.server.jsonrpc.JsonRpcSessions
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.sse.SseMcp
import org.http4k.mcp.server.sse.SseSessions
import org.http4k.mcp.server.stdio.StdIoMcpSessions
import org.http4k.mcp.server.websocket.WebsocketMcp
import org.http4k.mcp.server.websocket.WebsocketSessions
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
 *      /messages (POST) <-- receive messages from connected MCP clients
 */
fun mcpSse(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    SseMcp(
        McpProtocol(serverMetaData, SseSessions().apply { start() }, *capabilities)
    )

/**
 * Create an HTTP MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /ws <-- setup the WS connection to an MCP client
 */
fun mcpWebsocket(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    WebsocketMcp(McpProtocol(serverMetaData, WebsocketSessions().apply { start() }, *capabilities))

/**
 * Create an HTTP (pure JSONRPC) MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /jsonrpc (POST) <-- receive messages from connected MCP clients
 */
fun mcpJsonRpc(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    JsonRpcMcp(McpProtocol(serverMetaData, JsonRpcSessions(), *capabilities))

/**
 * Create an HTTP (+ SSE) MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /mcp (accept EventStream) <-- setup streaming connection to an MCP client
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 *      /mcp (DELETE) <-- delete a session
 */
fun mcpHttpStreaming(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    HttpStreamingMcp(
        McpProtocol(serverMetaData, HttpStreamingSessions().apply { start() }, *capabilities)
    )

/**
 * Create an HTTP (non-streaming) MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 */
fun mcpHttpNonStreaming(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    HttpNonStreamingMcp(
        McpProtocol(serverMetaData, HttpStreamingSessions().apply { start() }, *capabilities)
    )

/**
 * Create a StdIO MCP app from a set of feature bindings.
 */
fun mcpStdIo(
    serverMetaData: ServerMetaData,
    vararg capabilities: ServerCapability,
    reader: Reader = System.`in`.reader(),
    writer: Writer = System.out.writer(),
    executor: SimpleScheduler = SimpleSchedulerService(1)
) = McpProtocol(
    serverMetaData,
    StdIoMcpSessions(writer),
    ServerTools(capabilities.filterIsInstance<ToolCapability>()),
    ServerResources(capabilities.filterIsInstance<ResourceCapability>()),
    ServerPrompts(capabilities.filterIsInstance<PromptCapability>()),
    ServerCompletions(capabilities.filterIsInstance<CompletionCapability>()),
).apply {
    executor.readLines(reader) {
        try {
            receive(Unit, Session(SessionId.of(UUID(0, 0).toString())), Request(POST, "").body(it))
        } catch (e: Exception) {
            e.printStackTrace(System.err)
        }
    }
}

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)

fun compose(vararg bindings: ServerCapability) = CapabilityPack(bindings = bindings)
