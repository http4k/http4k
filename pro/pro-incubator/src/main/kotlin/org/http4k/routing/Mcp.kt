package org.http4k.routing

import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.mcp.McpBinding
import org.http4k.mcp.McpHandler
import org.http4k.mcp.PromptBinding
import org.http4k.mcp.Prompts
import org.http4k.mcp.ResourceBinding
import org.http4k.mcp.Resources
import org.http4k.mcp.ToolBinding
import org.http4k.mcp.Tools

fun mcp(implementation: Implementation, protocolVersion: ProtocolVersion, vararg bindings: McpBinding) = McpHandler(
    implementation,
    protocolVersion,
    ServerCapabilities(),
    Tools(bindings.filterIsInstance<ToolBinding>()),
    Resources(bindings.filterIsInstance<ResourceBinding>()),
    Prompts(bindings.filterIsInstance<PromptBinding>())
)
