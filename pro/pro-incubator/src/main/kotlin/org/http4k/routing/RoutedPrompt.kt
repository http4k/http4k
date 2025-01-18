package org.http4k.routing

import org.http4k.core.Request
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.protocol.McpPrompt

class RoutedPrompt(val prompt: Prompt, val handler: PromptHandler) : McpRouting {
    fun toPrompt() = prompt

    operator fun invoke(arguments: Map<String, String>, connectRequest: Request) =
        handler(PromptRequest(arguments, connectRequest)).let { McpPrompt.Get.Response(it.messages, it.description) }
}
