package org.http4k.connect.mcp

import org.http4k.connect.mcp.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.PromptBinding
import org.http4k.mcp.ResourceBinding
import org.http4k.mcp.ResourceTemplateBinding
import org.http4k.mcp.ToolBinding
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val mcpServer = mcp(
        Implementation("mcp-kotlin test server", Version.of("0.1.0")),
        LATEST_VERSION,
        PromptBinding("prompt1", "description1"),
        PromptBinding("prompt2", "description1"),
        ResourceBinding(Uri.of("https://http4k.org")),
        ToolBinding("reverse", "description", Reverse("name")) { listOf(Content.Text(it.input.reversed())) },
        ToolBinding("count", "description", Multiply(1, 2)) { listOf(Content.Text(it.first + it.second)) },
        ResourceTemplateBinding(Uri.of("https://{+subdomain}.http4k.org/{+path}")),
    )

    mcpServer.debug(debugStream = true).asServer(Helidon(3001)).start()
}

data class Reverse(val input: String)
data class Multiply(val first: Int, val second: Int)
