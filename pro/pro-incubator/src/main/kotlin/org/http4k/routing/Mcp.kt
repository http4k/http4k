package org.http4k.routing

import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Tools
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Tool
import org.http4k.mcp.server.McpCompletions
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.ServerMetaData

/**
 * Create a simple MCP server from a set of feature bindings.
 */
fun mcp(serverMetaData: ServerMetaData, vararg bindings: FeatureBinding) = McpHandler(
    serverMetaData,
    Prompts(bindings.filterIsInstance<PromptFeatureBinding>()),
    Tools(bindings.filterIsInstance<ToolFeatureBinding<*>>()),
    Resources(bindings.filterIsInstance<ResourceFeatureBinding>()),
    Roots(),
    McpCompletions()
)

infix fun <INPUT : Any> Tool<INPUT>.bind(other: ToolHandler<INPUT>) = ToolFeatureBinding(this, other)
infix fun Prompt.bind(other: PromptHandler) = PromptFeatureBinding(this, other)
infix fun Resource.bind(other: ResourceHandler) = ResourceFeatureBinding(this, other)
