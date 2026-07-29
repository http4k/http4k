/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.stateless.PromptRequest
import org.http4k.ai.mcp.stateless.PromptResponse
import org.http4k.ai.mcp.stateless.model.Content.Text
import org.http4k.ai.mcp.stateless.model.Message
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.model.Role
import org.http4k.routing.stateless.bind
import org.junit.jupiter.api.Test

class InMemoryPromptsTest {

    @Test
    fun `can invoke prompt by name with a PromptRequest`() {
        val prompt = Prompt("my-prompt", "a prompt")
        val expected = PromptResponse.Ok(Message(Role.Assistant, Text("hello")))
        val allPrompts = prompts(prompt bind { expected })

        val handler = allPrompts(prompt.name)
        val response = handler(PromptRequest())

        assertThat(response, equalTo(expected))
    }
}
