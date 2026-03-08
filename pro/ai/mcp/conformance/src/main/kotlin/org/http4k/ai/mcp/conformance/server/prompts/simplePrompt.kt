/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.routing.bind

fun simplePrompt() = Prompt("test_simple_prompt", "test_simple_prompt", title = "Simple Test Prompt") bind {
    PromptResponse(listOf(Message(Role.User, Content.Text("This is a simple prompt for testing."))))
}
