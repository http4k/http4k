package org.http4k.routing

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
import org.http4k.mcp.server.jsonrpc.StandardJsonRpcMcp
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.jsonrpc.JsonRpcClientSessions
import org.http4k.mcp.server.sse.SseClientSessions
import org.http4k.mcp.server.sse.StandardSseMcp
import org.http4k.mcp.server.stdio.StdIoMcpClientSessions
import org.http4k.mcp.server.ws.StandardWsMcp
import org.http4k.mcp.server.ws.WsClientSessions
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
    StandardSseMcp(
        McpProtocol(SseClientSessions().also { it.start() }, serverMetaData, *capabilities)
    )

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpWs(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) =
    StandardWsMcp(McpProtocol(WsClientSessions(), serverMetaData, *capabilities))

/**
 * Create an HTTP (pure JSONRPC) MCP app from a set of feature bindings.
 */
fun mcpJsonRpc(mcpEntity: McpEntity, version: Version, vararg capabilities: ServerCapability) =
    StandardJsonRpcMcp(
        McpProtocol(JsonRpcClientSessions(), ServerMetaData(mcpEntity, version), *capabilities)
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
    McpProtocol(
        serverMetaData,
        // TODO
//            .start(SimpleSchedulerService(1))
        StdIoMcpClientSessions(reader, writer),
        Tools(capabilities.filterIsInstance<ToolCapability>()),
        Resources(capabilities.filterIsInstance<ResourceCapability>()),
        Prompts(capabilities.filterIsInstance<PromptCapability>()),
        Completions(capabilities.filterIsInstance<CompletionCapability>()),
    )
}

/**
 * Create Tool capability by binding the Spec to the Handler.
 */
infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)

fun compose(vararg bindings: ServerCapability) = CapabilityPack(bindings = bindings)
