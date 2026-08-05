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
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.CacheScope.private
import org.http4k.ai.mcp.model.CacheScope.public
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.TtlMs
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class ListCacheHintsTest {

    private val ttl = TtlMs.of(5000)
    private val http = Request(GET, "/")

    @Test
    fun `tools-list carries the factory ttl and cacheScope`() {
        val result = tools(Tool("t", "d") bind { ToolResponse.Ok("x") }, ttlMs = ttl, cacheScope = private)
            .list(McpTool.List.Request.Params(), NoOp, http)

        assertThat(result.ttlMs, equalTo(ttl))
        assertThat(result.cacheScope, equalTo(private))
    }

    @Test
    fun `prompts-list carries the factory ttl and cacheScope`() {
        val result = prompts(
            Prompt(PromptName.of("p"), "d") bind { PromptResponse.Ok(listOf(Message(Assistant, Text("hi")))) },
            ttlMs = ttl, cacheScope = private
        ).list(McpPrompt.List.Request.Params(), NoOp, http)

        assertThat(result.ttlMs, equalTo(ttl))
        assertThat(result.cacheScope, equalTo(private))
    }

    @Test
    fun `resources-list and templates-list carry the factory ttl and cacheScope`() {
        val resources = resources(
            Resource.Static(Uri.of("res://s"), ResourceName.of("s"), "d") bind {
                ResourceResponse.Ok(listOf(Resource.Content.Text("hi", Uri.of("res://s"))))
            },
            ttlMs = ttl, cacheScope = private
        )

        val list = resources.listResources(McpResource.List.Request.Params(), NoOp, http)
        val templates = resources.listTemplates(McpResource.ListTemplates.Request.Params(), NoOp, http)

        assertThat(list.ttlMs, equalTo(ttl))
        assertThat(list.cacheScope, equalTo(private))
        assertThat(templates.ttlMs, equalTo(ttl))
        assertThat(templates.cacheScope, equalTo(private))
    }

    @Test
    fun `defaults to public with zero ttl`() {
        val result = tools(Tool("t", "d") bind { ToolResponse.Ok("x") })
            .list(McpTool.List.Request.Params(), NoOp, http)

        assertThat(result.ttlMs, equalTo(TtlMs.of(0)))
        assertThat(result.cacheScope, equalTo(public))
    }
}
