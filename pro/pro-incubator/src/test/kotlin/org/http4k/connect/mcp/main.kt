package org.http4k.connect.mcp

import org.http4k.connect.mcp.protocol.Implementation
import org.http4k.connect.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.connect.mcp.protocol.Version
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.model.Role
import org.http4k.mcp.prompts.Content
import org.http4k.mcp.prompts.Prompt
import org.http4k.mcp.prompts.PromptResponse
import org.http4k.mcp.tools.Tool
import org.http4k.mcp.tools.ToolResponse
import org.http4k.routing.RoutedResource
import org.http4k.routing.RoutedResourceTemplate
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer


fun main() {
    val mcpServer = mcp(
        Implementation("mcp-kotlin test server", Version.of("0.1.0")),
        LATEST_VERSION,
        Prompt("prompt1", "description1") bind {
            PromptResponse(Role.assistant to Content.Text(it.input.toString()))
        },
        Prompt("prompt1", "description1") bind {
            PromptResponse(Role.assistant to Content.Text(it.input.toString()))
        },
        RoutedResource(Uri.of("https://http4k.org")),
        Tool("reverse", "description", Reverse("name")) bind {
            ToolResponse.Ok(listOf(Content.Text(it.input.input.reversed())))
        },
        Tool("count", "description", Multiply(1, 2)) bind {
            ToolResponse.Ok(listOf(Content.Text(it.input.first + it.input.second)))
        },
        RoutedResourceTemplate(Uri.of("https://{+subdomain}.http4k.org/{+path}")),
    )

    mcpServer.debug(debugStream = true).asServer(Helidon(3001)).start()
}

data class Reverse(val input: String)
data class Multiply(val first: Int, val second: Int)
