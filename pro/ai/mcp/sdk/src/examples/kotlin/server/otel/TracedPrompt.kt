package server.otel

import org.http4k.ai.mcp.PromptFilter
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.then
import org.http4k.ai.model.Role
import org.http4k.routing.bind

fun TracedPrompt(filter: (PromptName) -> PromptFilter): ServerCapability {
    val promptName = PromptName.of("prompt1")

    return filter(promptName).then(Prompt(promptName, "description1") bind {
        PromptResponse(listOf(Message(Role.Assistant, Content.Text(it.toString()))), "description")
    })
}
