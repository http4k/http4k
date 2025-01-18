package org.http4k.routing

import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.ServerMetaData

/**
 * Create a simple MCP server from a set of feature bindings.
 */
fun mcp(serverMetaData: ServerMetaData, vararg bindings: FeatureBinding) = McpHandler(
    metaData = serverMetaData,
    prompts = Prompts(bindings.filterIsInstance<PromptFeatureBinding>()),
    tools = Tools(bindings.filterIsInstance<ToolFeatureBinding<*>>()),
    resources = Resources(bindings.filterIsInstance<ResourceFeatureBinding>()),
    completions = Completions(bindings.filterIsInstance<CompletionFeatureBinding>()),
    sampling = Sampling(bindings.filterIsInstance<SamplingFeatureBinding>()),
    roots = Roots()
)

infix fun <INPUT : Any> Tool<INPUT>.bind(handler: ToolHandler<INPUT>) = ToolFeatureBinding(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptFeatureBinding(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceFeatureBinding(this, handler)
infix fun ModelSelector.bind(handler: SamplingHandler) = SamplingFeatureBinding(this, handler)
