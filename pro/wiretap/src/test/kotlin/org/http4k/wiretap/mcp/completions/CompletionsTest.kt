/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.completions

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.model.Role
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test

class CompletionsTest {

    private val templates = Templates()
    private val renderer = DatastarElementRenderer(templates)

    private val mcpClient = mcp(
        ServerMetaData("test", "1.0"),
        NoMcpSecurity,
        Prompt("test-prompt", "a test prompt", Prompt.Arg.required("city")) bind {
            PromptResponse.Ok(Role.Assistant, "hello")
        },
        Reference.Prompt("test-prompt") bind {
            CompletionResponse.Ok(listOf("alpha", "beta", "gamma"))
        }
    ).testMcpClient(Request(GET, "/mcp"))

    private val function = Completions(mcpClient.apply { start() })

    @Test
    fun `tab content lists prompts and templates separately`() {
        val response = function.http(renderer, templates)(Request(GET, "/completions"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("Prompts"))
        assertThat(response.bodyString(), containsSubstring("test-prompt"))
    }

    @Test
    fun `inspect shows detail with argument dropdown for prompt reference`() {
        val response = function.http(renderer, templates)(Request(GET, "/completions/prompts/test-prompt"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("test-prompt"))
        assertThat(response.bodyString(), containsSubstring("city"))
    }

    @Test
    fun `complete returns completion values`() {
        val response = function.http(renderer, templates)(
            Request(POST, "/completions/complete")
                .header("Content-Type", "application/json")
                .body("""{"refType":"prompt","refName":"test-prompt","argumentName":"city","argumentValue":"al"}""")
        )
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("alpha"))
    }
}
