package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.model.Prompt
import org.http4k.routing.bind

fun dynamicPrompt() = Prompt("test_dynamic_prompt", "test_dynamic_prompt", title = "Dynamic Prompt") bind {
    throw NotImplementedError()
}
