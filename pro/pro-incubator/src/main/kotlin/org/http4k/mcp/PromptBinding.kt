package org.http4k.mcp

import org.http4k.connect.mcp.Content
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Role

class PromptBinding(val name: String, private val description: String?) : McpBinding {
    fun toPrompt() = Prompt(
        name, description, listOf(
            Prompt.Argument("p1", "d1", true),
            Prompt.Argument("p2", "d2", false)
        )
    )

    fun toMessages(arguments: Map<String, String>) = listOf(
        Prompt.Message(
            Role.assistant,
            Content.Text(arguments.toString())
        )
    )
}
