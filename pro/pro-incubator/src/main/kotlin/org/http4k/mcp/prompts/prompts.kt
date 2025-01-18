package org.http4k.mcp.prompts

import org.http4k.core.Request
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Role

typealias PromptHandler = (PromptRequest) -> PromptResponse

data class PromptRequest(val input: Map<String, String>, val connectRequest: Request)

data class PromptResponse(val messages: List<Pair<Role, Content>>) {
    constructor(vararg messages: Pair<Role, Content>) : this(messages.toList())
}
