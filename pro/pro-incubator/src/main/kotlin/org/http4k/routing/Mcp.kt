package org.http4k.routing

import org.http4k.mcp.PromptHandler
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.Implementation
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.server.McpCompletions
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.McpPrompts
import org.http4k.mcp.server.McpResourceTemplates
import org.http4k.mcp.server.McpResources
import org.http4k.mcp.server.McpRoots
import org.http4k.mcp.server.McpTools

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
