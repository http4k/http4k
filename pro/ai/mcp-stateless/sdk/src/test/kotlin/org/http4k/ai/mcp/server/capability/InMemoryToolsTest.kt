/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.model.ToolName
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class InMemoryToolsTest {

    @Test
    fun `can invoke tool by name with a ToolRequest`() {
        val allTools = tools(Tool("my-tool", "a tool") bind { ToolResponse.Ok("hello") })

        val handler = allTools(ToolName.of("my-tool"))
        val response = handler(ToolRequest())

        assertThat(response, equalTo(ToolResponse.Ok("hello")))
    }
}
