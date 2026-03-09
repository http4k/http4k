package wiretap.examples

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.model.long
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.Role
import org.http4k.routing.bind
import org.http4k.routing.mcp

fun McpServer() = mcp(
    ServerMetaData("mcp server", "0.0.0").withExtensions(McpApps),
    NoMcpSecurity,
    ToolWithArgs(),
    PromptWithArguments()
)

private fun ToolWithArgs(): ToolCapability {
    val first = Tool.Arg.long().required("first", "first number")
    val second = Tool.Arg.long().optional("second", "second number")

    return Tool(
        "add-numbers", "add some numbers!",
        first,
        second
    ) bind { Ok("${first(it) + (second(it) ?: 0)}") }
}

private fun PromptWithArguments(): PromptCapability {
    val name = Prompt.Arg.required("name", "the name of the person to greet")
    return Prompt(
        "greet", "say hello to my little friend",
        name
    ) bind { PromptResponse(Role.Assistant, "hello ${name(it)}") }
}
