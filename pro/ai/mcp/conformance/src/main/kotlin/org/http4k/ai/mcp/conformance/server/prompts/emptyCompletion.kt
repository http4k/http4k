package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.model.Reference
import org.http4k.routing.bind

fun emptyCompletion() = Reference.Prompt("test_prompt_with_arguments") bind {
    CompletionResponse(listOf())
}
