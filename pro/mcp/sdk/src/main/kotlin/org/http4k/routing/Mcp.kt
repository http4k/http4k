package org.http4k.routing

import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.sse.SseMcpProtocol
import org.http4k.mcp.stdio.StdIoMcpProtocol
import org.http4k.mcp.stdio.pipeMessagesToFromStdIo

/**
 * Create an HTTP MCP app from a set of feature bindings.
 */
fun mcpHttp(serverMetaData: ServerMetaData, vararg bindings: FeatureBinding) = McpHandler(
    SseMcpProtocol(
        serverMetaData,
        Prompts(bindings.filterIsInstance<PromptFeatureBinding>()),
        Tools(bindings.filterIsInstance<ToolFeatureBinding<*>>()),
        Resources(bindings.filterIsInstance<ResourceFeatureBinding>()),
        Completions(bindings.filterIsInstance<CompletionFeatureBinding>()),
        Sampling(bindings.filterIsInstance<SamplingFeatureBinding>())
    )
)

/**
 * Create a StdIO MCP app from a set of feature bindings.
 */
fun mcpStdIo(serverMetaData: ServerMetaData, vararg bindings: FeatureBinding) {
    SimpleSchedulerService(1).pipeMessagesToFromStdIo(
        StdIoMcpProtocol(
            serverMetaData,
            System.out.writer(),
            Prompts(bindings.filterIsInstance<PromptFeatureBinding>()),
            Tools(bindings.filterIsInstance<ToolFeatureBinding<*>>()),
            Resources(bindings.filterIsInstance<ResourceFeatureBinding>()),
            Completions(bindings.filterIsInstance<CompletionFeatureBinding>()),
            Sampling(bindings.filterIsInstance<SamplingFeatureBinding>()),
        ),
        System.`in`.reader()
    )()
}

infix fun <INPUT : Any> Tool<INPUT>.bind(handler: ToolHandler<INPUT>) = ToolFeatureBinding(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptFeatureBinding(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceFeatureBinding(this, handler)
infix fun ModelSelector.bind(handler: SamplingHandler) = SamplingFeatureBinding(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionFeatureBinding(this, handler)

