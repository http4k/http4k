/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.stateless.PromptRequest
import org.http4k.ai.mcp.stateless.PromptResponse
import org.http4k.ai.mcp.stateless.ResourceRequest
import org.http4k.ai.mcp.stateless.ResourceResponse
import org.http4k.ai.mcp.stateless.client.http.HttpMcpClient
import org.http4k.ai.mcp.stateless.model.Content.Text
import org.http4k.ai.mcp.stateless.model.Message
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.ResourceName
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.core.Uri
import org.http4k.routing.stateless.bind
import org.http4k.routing.stateless.mcp
import org.junit.jupiter.api.Test

class CacheHintsTest {

    private val resource = Resource.Static(Uri.of("res://cfg"), ResourceName.of("cfg")) bind {
        ResourceResponse.Ok(listOf(Resource.Content.Text("hi", it.uri)), ttlMs = TtlMs.of(300_000))
    }

    private val prompt = Prompt(PromptName.of("greet"), "greets") bind {
        PromptResponse.Ok(listOf(Message(Assistant, Text("hi"))), ttlMs = TtlMs.of(60_000))
    }

    private val server = mcp(ServerMetaData("cache-server", "1.0.0"), NoMcpSecurity, resource, prompt)
    private val client = HttpMcpClient(Uri.of("/mcp"), http = server.http!!)

    @Test
    fun `resources-read carries the handler's per-response ttl`() {
        val ok = client.resources().read(ResourceRequest(Uri.of("res://cfg"))).valueOrNull() as ResourceResponse.Ok

        assertThat(ok.ttlMs, equalTo(TtlMs.of(300_000)))
    }

    @Test
    fun `prompts-get carries the handler's per-response ttl`() {
        val ok = client.prompts().get(PromptName.of("greet"), PromptRequest()).valueOrNull() as PromptResponse.Ok

        assertThat(ok.ttlMs, equalTo(TtlMs.of(60_000)))
    }
}
