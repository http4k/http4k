package org.http4k.routing

import org.http4k.connect.mcp.protocol.McpPrompt
import org.http4k.core.Request
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.model.Prompt

class RoutedPrompt(val prompt: Prompt, val handler: PromptHandler) : McpRouting {
    fun toPrompt() = McpPrompt(
        prompt.name, prompt.description, listOf(
            McpPrompt.Argument("p1", "d1", true),
            McpPrompt.Argument("p2", "d2", false)
        )
    )

    operator fun invoke(arguments: Map<String, String>, connectRequest: Request) =
        McpPrompt.Get.Response(
            handler(PromptRequest(arguments, connectRequest)).messages.map { McpPrompt.Message(it.first, it.second) }
        )
}
