/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.LogLevel.info
import org.http4k.ai.mcp.model.LogLevel.warning
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.ToolName
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.MCP_NAME
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.lens.accept
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.junit.jupiter.api.Test

class RequestStreamingTest {

    // Request/response streaming lives on the HTTP face (the SSE face declines everything but subscriptions/listen),
    // delivered as a piped text/event-stream response body produced on a virtual thread.
    private val server = mcp(
        ServerMetaData("streamer", "1.0.0"),
        NoMcpSecurity,
        Tool("greet", "greets") bind { req ->
            req.client.progress("tok", 1, 2.0, "step1")
            req.client.progress("tok", 2, 2.0, "step2")
            req.client.log("noisy", info)      // below the requested warning threshold -> dropped
            req.client.log("important", warning)
            ToolResponse.Ok("done")
        }
    )

    private fun toolCall(logLevel: String?) = Request(POST, "/mcp")
        .accept(TEXT_EVENT_STREAM)
        .with(Header.MCP_PROTOCOL_VERSION of LATEST_VERSION)
        .with(Header.MCP_METHOD of McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("greet")), "1").method)
        .with(Header.MCP_NAME of "greet")
        .body(
            """{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"greet",""" +
                """"_meta":{"io.modelcontextprotocol/protocolVersion":"${LATEST_VERSION.value}",""" +
                """"io.modelcontextprotocol/clientCapabilities":{}${logLevel?.let { ""","io.modelcontextprotocol/logLevel":"$it"""" } ?: ""}}}}"""
        )

    private fun streamOf(logLevel: String?) = server.http!!(toolCall(logLevel)).bodyString()

    @Test
    fun `progress and gated logs stream before the result on the request's own response`() {
        val stream = streamOf(logLevel = "warning")

        assertThat(stream, containsSubstring("notifications/progress").and(containsSubstring("\"progress\":1")))
        assertThat(stream, containsSubstring("\"progress\":2"))
        // info log is below the requested `warning` level -> only the warning log appears
        assertThat(stream, containsSubstring("notifications/message").and(containsSubstring("important")))
        assertThat(stream.contains("noisy"), equalTo(false))
        assertThat(stream, containsSubstring("done"))
        // ordering: progress + log stream before the terminal result
        assertThat(stream.indexOf("important") < stream.indexOf("done"), equalTo(true))
    }

    @Test
    fun `with no logLevel declared, logs are not emitted`() {
        val stream = streamOf(logLevel = null)

        // progress still flows; the two log calls are dropped (no declared level)
        assertThat(stream, containsSubstring("\"progress\":1"))
        assertThat(stream, containsSubstring("\"progress\":2"))
        assertThat(stream.contains("notifications/message"), equalTo(false))
        assertThat(stream, containsSubstring("done"))
    }
}
