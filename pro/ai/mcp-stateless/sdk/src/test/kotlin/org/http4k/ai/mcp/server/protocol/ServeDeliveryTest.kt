/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.junit.jupiter.api.Test

class ServeDeliveryTest {

    private val server = mcp(
        ServerMetaData("deliverer", "1.0.0"),
        NoMcpSecurity,
        Tool("greet", "greets") bind { ToolResponse.Ok("hello") },
        Tool("needy", "needs sampling") bind { throw McpException(MissingRequiredClientCapabilityError(listOf("sampling"))) }
    )

    private fun toolCall(name: String = "greet", mcpMethod: String = "tools/call", accept: String = TEXT_EVENT_STREAM.value) =
        Request(POST, "/mcp")
            .header("Accept", accept)
            .header("mcp-protocol-version", LATEST_VERSION.value)
            .header("mcp-method", mcpMethod)
            .header("mcp-name", name)
            .body(
                """{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"$name",""" +
                    """"_meta":{"io.modelcontextprotocol/protocolVersion":"${LATEST_VERSION.value}",""" +
                    """"io.modelcontextprotocol/clientCapabilities":{}}}}"""
            )

    @Test
    fun `a mirror-header validation failure is delivered as application-json 400, even on an event-stream Accept`() {
        val response = server.http!!(toolCall(mcpMethod = "prompts/list")) // Mcp-Method != body method -> -32020

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.header("content-type"), present(containsSubstring(APPLICATION_JSON.value)))
        assertThat(response.bodyString(), containsSubstring("-32020"))
    }

    @Test
    fun `a json-only client gets a single application-json result`() {
        val response = server.http!!(toolCall(accept = APPLICATION_JSON.value))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("content-type"), present(containsSubstring(APPLICATION_JSON.value)))
        assertThat(response.bodyString(), containsSubstring("hello"))
    }

    @Test
    fun `a tool-capability rejection is delivered as application-json 400, even on an event-stream Accept`() {
        val response = server.http!!(toolCall(name = "needy")) // -32021 thrown inside the tool, event-stream Accept

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.header("content-type"), present(containsSubstring(APPLICATION_JSON.value)))
        assertThat(response.bodyString(), containsSubstring("-32021"))
    }

    @Test
    fun `an event-stream client gets a streamed text-event-stream result`() {
        val response = server.http!!(toolCall(accept = TEXT_EVENT_STREAM.value))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("content-type"), equalTo(TEXT_EVENT_STREAM.value))
        assertThat(response.bodyString(), containsSubstring("hello"))
    }
}
