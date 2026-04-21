/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.acceptance

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.mcp.testing.useClient
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.util.FixedClock
import org.http4k.wiretap.RemoteTarget
import org.http4k.wiretap.Wiretap
import org.http4k.wiretap.WiretapTarget
import org.http4k.wiretap.util.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpServerTest : WiretapSmokeContract {

    override val testRequest = Request(GET, Uri.of("/mcp"))

    private val server = mcp(
        ServerMetaData(McpEntity.of("123"), Version.of("123")),
        NoMcpSecurity,
        Tool("foo", "bar") bind { Ok("Hello") }
    )
        .asServer(Helidon(0))

    override lateinit var target: WiretapTarget

    @Test
    fun `mcp transactions through wiretap are stored`() {
        val wiretap = Wiretap(target, clock = FixedClock)

        HttpNonStreamingMcpClient(Uri.of("/mcp"), http = wiretap.http!!).apply { start() }.use {
            val list = it.tools().list().coerce<List<McpTool>>()
            assertThat(list.size, equalTo(1))
        }

        wiretap.testMcpClient(Request(POST, "_wiretap/mcp")).useClient {
            val call = tools().call(ToolName.of("list_transactions")).coerce<Ok>()

            val calls = call.content!![0] as Text
            val elements = Json.elements(Json.parse(calls.text))
            assertThat(elements.size, equalTo(4))
        }

    }

    @BeforeEach
    fun start() {
        server.start()
        target = RemoteTarget(server.uri())
    }

    @BeforeEach
    fun stop() {
        server.stop()
    }
}
