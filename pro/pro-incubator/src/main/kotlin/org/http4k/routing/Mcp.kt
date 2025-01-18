package org.http4k.routing

import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.mcp.Completions
import org.http4k.mcp.McpBinding
import org.http4k.mcp.McpHandler
import org.http4k.mcp.McpTools
import org.http4k.mcp.PromptBinding
import org.http4k.mcp.Prompts
import org.http4k.mcp.ResourceBinding
import org.http4k.mcp.ResourceTemplateBinding
import org.http4k.mcp.ResourceTemplates
import org.http4k.mcp.Resources
import org.http4k.mcp.Roots
import org.http4k.mcp.RoutedToolBinding
import org.http4k.mcp.tools.Tool
import org.http4k.mcp.tools.ToolHandler

fun mcp(implementation: Implementation, protocolVersion: ProtocolVersion, vararg bindings: McpBinding) = McpHandler(
    implementation,
    protocolVersion,
    ServerCapabilities(),
    Completions(),
    Roots(),
    McpTools(bindings.filterIsInstance<RoutedToolBinding<*>>()),
    Resources(bindings.filterIsInstance<ResourceBinding>()),
    ResourceTemplates(bindings.filterIsInstance<ResourceTemplateBinding>()),
    Prompts(bindings.filterIsInstance<PromptBinding>())
)

infix fun <INPUT: Any> Tool<INPUT>.bind(other: ToolHandler<INPUT>) = RoutedToolBinding(this, other)
