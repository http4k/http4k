package org.http4k.routing

import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.IncomingSamplingHandler
import org.http4k.mcp.OutgoingSamplingHandler
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.capability.CapabilityPack
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.IncomingSampling
import org.http4k.mcp.server.capability.IncomingSamplingCapability
import org.http4k.mcp.server.capability.OutgoingSampling
import org.http4k.mcp.server.capability.OutgoingSamplingCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.sse.SseMcpProtocol
import org.http4k.mcp.server.stdio.StdIoMcpProtocol
import java.io.Reader
import java.io.Writer

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpHttp(serverMetaData: ServerMetaData, vararg capabilities: ServerCapability) = McpHandler(
    SseMcpProtocol(
        serverMetaData,
        Prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        Tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        Resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        Completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        IncomingSampling(capabilities.flatMap { it }.filterIsInstance<IncomingSamplingCapability>()),
        OutgoingSampling(capabilities.flatMap { it }.filterIsInstance<OutgoingSamplingCapability>())
    )
        .also { it.start() }
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
        Prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        Tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        Resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        Completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        IncomingSampling(capabilities.flatMap { it }.filterIsInstance<IncomingSamplingCapability>()),
    ).start(SimpleSchedulerService(1))
}

/**
 * Create Tool capability by binding the Spec to the Handler.
 */
infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)
infix fun McpEntity.bind(handler: OutgoingSamplingHandler) = OutgoingSamplingCapability(this, handler)
infix fun ModelSelector.bind(handler: IncomingSamplingHandler) = IncomingSamplingCapability(this, handler)

fun compose(vararg bindings: ServerCapability) = CapabilityPack(bindings = bindings)
