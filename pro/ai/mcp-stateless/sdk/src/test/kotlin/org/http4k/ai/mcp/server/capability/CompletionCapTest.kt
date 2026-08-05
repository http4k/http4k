/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class CompletionCapTest {

    private val ref = Reference.Prompt("p")

    private fun completionFor(response: CompletionResponse.Ok) =
        (ref bind { response }).complete(
            McpCompletion.Request.Params(ref, CompletionArgument("a", "v")), NoOp, Request(GET, "/")
        ).completion

    @Test
    fun `caps values at 100 and flags hasMore when truncated`() {
        val completion = completionFor(CompletionResponse.Ok((1..150).map { "v$it" }))

        assertThat(completion.values.size, equalTo(100))
        assertThat(completion.hasMore, equalTo(true))
    }

    @Test
    fun `leaves a small result and the handler's hasMore or total untouched`() {
        val completion = completionFor(CompletionResponse.Ok(listOf("a", "b"), total = 2, hasMore = false))

        assertThat(completion.values, equalTo(listOf("a", "b")))
        assertThat(completion.total, equalTo(2))
        assertThat(completion.hasMore, equalTo(false))
    }
}
