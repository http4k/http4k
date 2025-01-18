package org.http4k.connect.mcp

import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.Implementation
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.Version
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
            PromptResponse("description", listOf(Message(Role.assistant, Content.Text(it.input.toString()))))
        },
        Prompt("prompt1", "description1") bind {
            PromptResponse("description", listOf(Message(Role.assistant, Content.Text(it.input.toString()))))
        },
        Resource(Uri.of("https://http4k.org"), "HTTP4K", "description") bind LinksOnPage(JavaHttpClient()),
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
