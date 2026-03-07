package wiretap.examples

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.Role
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming

fun McpServer() = mcpHttpStreaming(
    ServerMetaData("mcp server", "0.0.0").withExtensions(McpApps),
    NoMcpSecurity,
    Tool("tool", "") bind { Ok("hello") },
    Prompt("prompt", "") bind { PromptResponse(Role.Assistant, "hello") }
)
