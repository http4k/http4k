/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.CacheScope.private
import org.http4k.ai.mcp.model.CacheScope.public
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class CacheScopeTest {

    @Test
    fun `resources-read result takes cacheScope from the capability, not the response`() {
        val cap = ResourceCapability(Resource.Static(Uri.of("res://cfg"), ResourceName.of("cfg"), cacheScope = private)) {
            ResourceResponse.Ok(listOf(Resource.Content.Text("hi", Uri.of("res://cfg"))))
        }

        val result = cap.read(McpResource.Read.Request.Params(Uri.of("res://cfg")), NoOp, Request(GET, "/"))

        assertThat(result.cacheScope, equalTo(private))
    }

    @Test
    fun `prompts-get result takes cacheScope from the capability, defaulting to public`() {
        val cap = PromptCapability(Prompt(PromptName.of("greet"), "greets")) {
            PromptResponse.Ok(listOf(Message(Assistant, Text("hi"))))
        }

        val result = cap.get(McpPrompt.Get.Request.Params(PromptName.of("greet")), NoOp, Request(GET, "/"))

        assertThat(result.cacheScope, equalTo(public))
    }
}
