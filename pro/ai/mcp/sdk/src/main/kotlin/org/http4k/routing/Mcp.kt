package org.http4k.routing

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.PromptHandler
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ToolHandler
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.http.HttpNonStreamingMcp
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.jsonrpc.JsonRpcMcp
import org.http4k.ai.mcp.server.jsonrpc.JsonRpcSessions
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.ai.mcp.server.stdio.StdIoMcpSessions
import org.http4k.ai.mcp.server.websocket.WebsocketMcp
import org.http4k.ai.mcp.server.websocket.WebsocketSessions
import org.http4k.core.Method.POST
import org.http4k.core.Request
import java.io.Reader
import java.io.Writer
import java.time.Duration.ZERO
import java.util.UUID

/**
 * Create an HTTP (+ SSE) MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /mcp (accept EventStream) <-- setup streaming connection to an MCP client
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 *      /mcp (DELETE) <-- delete a session
 */
fun mcpHttpStreaming(metadata: ServerMetaData, security: McpSecurity, vararg capabilities: ServerCapability) =
    HttpStreamingMcp(McpProtocol(metadata, HttpSessions().apply { start() }, *capabilities), security)

/**
 * Create an HTTP (non-streaming) MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 */
fun mcpHttpNonStreaming(metadata: ServerMetaData, security: McpSecurity, vararg capabilities: ServerCapability) =
    HttpNonStreamingMcp(McpProtocol(metadata, HttpSessions().apply { start() }, *capabilities), security)

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
fun mcpSse(metadata: ServerMetaData, security: McpSecurity, vararg capabilities: ServerCapability) =
    SseMcp(McpProtocol(metadata, SseSessions().apply { start() }, *capabilities), security)

/**
 * Create an HTTP MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /ws <-- setup the WS connection to an MCP client
 */
fun mcpWebsocket(metadata: ServerMetaData, security: McpSecurity, vararg capabilities: ServerCapability) =
    WebsocketMcp(McpProtocol(metadata, WebsocketSessions().apply { start() }, *capabilities), security)

/**
 * Create an HTTP (pure JSONRPC) MCP app from a set of feature bindings.
 *
 *  The standard paths used are:
 *      /jsonrpc (POST) <-- receive messages from connected MCP clients
 */
fun mcpJsonRpc(metadata: ServerMetaData, security: McpSecurity, vararg capabilities: ServerCapability) =
    JsonRpcMcp(McpProtocol(metadata, JsonRpcSessions(), *capabilities), security)

/**
 * Create a StdIO MCP app from a set of feature bindings.
 */
fun mcpStdIo(
    metadata: ServerMetaData,
    vararg capabilities: ServerCapability,
    reader: Reader = System.`in`.reader(),
    writer: Writer = System.out.writer(),
    executor: SimpleScheduler = SimpleSchedulerService(1)
) = McpProtocol(
    metadata,
    StdIoMcpSessions(writer),
    ServerTools(capabilities.filterIsInstance<ToolCapability>()),
    ServerResources(capabilities.filterIsInstance<ResourceCapability>()),
    ServerPrompts(capabilities.filterIsInstance<PromptCapability>()),
    ServerCompletions(capabilities.filterIsInstance<CompletionCapability>()),
).apply {
    executor.schedule({
        reader.buffered().lineSequence().forEach { it: String ->
            try {
                receive(Unit, Session(SessionId.of(UUID(0, 0).toString())), Request(POST, "").body(it))
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }
    }, ZERO)
}

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)

fun compose(vararg bindings: ServerCapability) = CapabilityPack(bindings = bindings)
