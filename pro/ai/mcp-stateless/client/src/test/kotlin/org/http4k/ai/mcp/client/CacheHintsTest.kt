/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.client.http.HttpMcpClient
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.TtlMs
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp
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
