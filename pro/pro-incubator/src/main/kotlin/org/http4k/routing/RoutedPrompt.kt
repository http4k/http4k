package org.http4k.routing

import org.http4k.connect.mcp.Content
import org.http4k.connect.mcp.McpPrompt
import org.http4k.connect.mcp.Role

class RoutedPrompt(val name: String, private val description: String?) : McpRouting {
    fun toPrompt() = McpPrompt(
        name, description, listOf(
            McpPrompt.Argument("p1", "d1", true),
            McpPrompt.Argument("p2", "d2", false)
        )
    )

    fun toMessages(arguments: Map<String, String>) = listOf(
        McpPrompt.Message(
            Role.assistant,
            Content.Text(arguments.toString())
        )
    )
}
