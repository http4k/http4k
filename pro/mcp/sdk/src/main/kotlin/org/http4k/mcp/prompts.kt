package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.Message

/**
 * A PromptHandler is a function which creates a Prompt from a set of inputs
 */
typealias PromptHandler = (PromptRequest) -> PromptResponse

data class PromptRequest(
    private val args: Map<String, String> = emptyMap(),
    val connectRequest: Request? = null
) : Map<String, String> by args

data class PromptResponse(val messages: List<Message>, val description: String? = null)
