/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.model.Reference
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class InMemoryCompletionsTest {

    @Test
    fun `can invoke completion by reference with a CompletionRequest`() {
        val ref = Reference.Prompt("my-prompt")
        val expected = CompletionResponse.Ok(listOf("foo", "bar"))
        val allCompletions = completions(ref bind { expected })

        val handler = allCompletions(ref)
        val response = handler(CompletionRequest("arg", "value"))

        assertThat(response, equalTo(expected))
    }
}
