package org.http4k.mcp

import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Prompt.Content
import org.http4k.connect.mcp.Prompt.Message
import org.http4k.connect.mcp.Role

sealed interface McpBinding

class ToolBinding : McpBinding
class ResourceBinding : McpBinding

class PromptBinding(val name: String, private val description: String?) : McpBinding {
    fun toPrompt() = Prompt(
        name, description, listOf(
            Prompt.Argument("p1", "d1", true),
            Prompt.Argument("p2", "d2", false)
        )
    )

    fun toMessages(arguments: Map<String, String>) = listOf(
        Message(
            Role.assistant,
            Content.Text(arguments.toString())
        )
    )
}

