package org.http4k.routing

import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.mcp.McpCompletions
import org.http4k.mcp.McpHandler
import org.http4k.mcp.McpPrompts
import org.http4k.mcp.McpResourceTemplates
import org.http4k.mcp.McpResources
import org.http4k.mcp.McpRoots
import org.http4k.mcp.McpTools
import org.http4k.mcp.prompts.Prompt
import org.http4k.mcp.prompts.PromptHandler
import org.http4k.mcp.tools.Tool
import org.http4k.mcp.tools.ToolHandler

fun mcp(implementation: Implementation, protocolVersion: ProtocolVersion, vararg bindings: McpRouting) = McpHandler(
    implementation,
    protocolVersion,
    ServerCapabilities(),
    McpCompletions(),
    McpRoots(),
    McpTools(bindings.filterIsInstance<RoutedTool<*>>()),
    McpResources(bindings.filterIsInstance<RoutedResource>()),
    McpResourceTemplates(bindings.filterIsInstance<RoutedResourceTemplate>()),
    McpPrompts(bindings.filterIsInstance<RoutedPrompt>())
)

infix fun <INPUT : Any> Tool<INPUT>.bind(other: ToolHandler<INPUT>) = RoutedTool(this, other)
infix fun Prompt.bind(other: PromptHandler) = RoutedPrompt(this, other)
