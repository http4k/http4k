/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.prompts

import org.http4k.ai.mcp.stateless.PromptResponse
import org.http4k.ai.mcp.stateless.model.Content
import org.http4k.ai.mcp.stateless.model.Message
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.routing.stateless.bind

fun simplePrompt() = Prompt("test_simple_prompt", "test_simple_prompt", title = "Simple Test Prompt") bind {
    PromptResponse.Ok(listOf(Message(Role.User, Content.Text("This is a simple prompt for testing."))))
}
