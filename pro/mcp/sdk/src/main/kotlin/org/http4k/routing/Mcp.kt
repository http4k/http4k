package org.http4k.routing

import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.IncomingSamplingHandler
import org.http4k.mcp.OutgoingSamplingHandler
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.capability.CapabilityBinding
import org.http4k.mcp.capability.CompletionBinding
import org.http4k.mcp.capability.Completions
import org.http4k.mcp.capability.IncomingSampling
import org.http4k.mcp.capability.IncomingSamplingBinding
import org.http4k.mcp.capability.OutgoingSampling
import org.http4k.mcp.capability.OutgoingSamplingBinding
import org.http4k.mcp.capability.PromptBinding
import org.http4k.mcp.capability.Prompts
import org.http4k.mcp.capability.ResourceBinding
import org.http4k.mcp.capability.Resources
import org.http4k.mcp.capability.ToolBinding
import org.http4k.mcp.capability.Tools
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.sse.SseMcpProtocol
import org.http4k.mcp.stdio.StdIoMcpProtocol
import org.http4k.mcp.stdio.pipeMessagesToFromStdIo
import java.io.Reader
import java.io.Writer

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpHttp(serverMetaData: ServerMetaData, vararg bindings: CapabilityBinding) = McpHandler(
    SseMcpProtocol(
        serverMetaData,
        Prompts(bindings.filterIsInstance<PromptBinding>()),
        Tools(bindings.filterIsInstance<ToolBinding>()),
        Resources(bindings.filterIsInstance<ResourceBinding>()),
        Completions(bindings.filterIsInstance<CompletionBinding>()),
        IncomingSampling(bindings.filterIsInstance<IncomingSamplingBinding>()),
        OutgoingSampling(bindings.filterIsInstance<OutgoingSamplingBinding>())
    )
)

/**
 * Create a StdIO MCP app from a set of feature bindings.
 */
fun mcpStdIo(
    serverMetaData: ServerMetaData, vararg bindings: CapabilityBinding,
    reader: Reader = System.`in`.reader(),
    writer: Writer = System.out.writer(),
) {
    SimpleSchedulerService(1).pipeMessagesToFromStdIo(
        StdIoMcpProtocol(
            serverMetaData,
            writer,
            Prompts(bindings.filterIsInstance<PromptBinding>()),
            Tools(bindings.filterIsInstance<ToolBinding>()),
            Resources(bindings.filterIsInstance<ResourceBinding>()),
            Completions(bindings.filterIsInstance<CompletionBinding>()),
            IncomingSampling(bindings.filterIsInstance<IncomingSamplingBinding>()),
        ),
        reader
    )()
}

infix fun Tool.bind(handler: ToolHandler) = ToolBinding(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptBinding(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceBinding(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionBinding(this, handler)
infix fun McpEntity.bind(handler: OutgoingSamplingHandler) = OutgoingSamplingBinding(this, handler)
infix fun ModelSelector.bind(handler: IncomingSamplingHandler) = IncomingSamplingBinding(this, handler)
