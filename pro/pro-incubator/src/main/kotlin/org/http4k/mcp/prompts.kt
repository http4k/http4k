package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.Message

typealias PromptHandler = (PromptRequest) -> PromptResponse

data class PromptRequest(val input: Map<String, String>, val connectRequest: Request)

data class PromptResponse(val description: String, val messages: List<Message>)
