/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse.Ok
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.capabilities
import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class McpInterceptTest {

    private val uri = Uri.of("ui://a-ui")

    @RegisterExtension
    val intercept = Intercept.mcp(Always) {
        capabilities(
            Tool("non_app", "") bind { ToolResponse.Ok("hello") },
            Resource.Static(uri, ResourceName.of("example ui")) bind { Ok(Text("hello world", uri)) },
            Prompt("prompt", "", Prompt.Arg.required("city")) bind { PromptResponse.Ok(Role.Assistant, "hello") }
        )
    }

    @Test
    fun `can pass through an mcp client`(mcpClient: McpClient) {
        mcpClient.run {
            start()
            assertThat(
                resources().read(ResourceRequest(uri)).coerce<Ok>().list.first().uri,
                equalTo(uri)
            )
            assertThat(
                tools().call(ToolName.of("non_app")).coerce<ToolResponse.Ok>().content?.first().toString(),
                containsSubstring("hello")
            )
            assertThat(tools().list().coerce<List<McpTool>>().size, greaterThan(0))
            assertThat(prompts().list().coerce<List<McpPrompt>>().size, greaterThan(0))
        }
    }
}
