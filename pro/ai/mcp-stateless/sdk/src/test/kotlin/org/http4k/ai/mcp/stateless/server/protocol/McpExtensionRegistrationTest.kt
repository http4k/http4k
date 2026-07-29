/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.routing.stateless.mcp
import org.http4k.routing.stateless.mcpHttpNonStreaming
import org.junit.jupiter.api.Test

class McpExtensionRegistrationTest {

    private val unrouted = McpRpcMethod.of("notifications/tools/list_changed")

    private fun protocol(vararg extensions: McpServerExtension) =
        McpProtocol(ServerMetaData("test-server", "1.0.0"), extensions = extensions.toList())

    @Test
    fun `a registered extension is advertised in the discover capabilities`() {
        val response = protocol(FakeExtension())(request("server/discover"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring(""""org.http4k/fake":{"greeting":"hello"}"""))
    }

    @Test
    fun `an unregistered extension is not advertised`() {
        assertThat(protocol()(request("server/discover")).bodyString(), !containsSubstring("org.http4k/fake"))
    }

    @Test
    fun `a registered extension serves the methods it claims`() {
        val response = protocol(FakeExtension(claims = unrouted))(request(unrouted.value))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring(MARKER))
    }

    @Test
    fun `an otherwise-unrouted method is still accepted when no extension claims it`() {
        assertThat(protocol()(request(unrouted.value)).status, equalTo(ACCEPTED))
    }

    @Test
    fun `a registered extension cannot claim a method the server already routes`() {
        val response = protocol(FakeExtension(claims = McpRpcMethod.of("tools/list")))(request("tools/list"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), !containsSubstring(MARKER))
        assertThat(response.bodyString(), containsSubstring("tools"))
    }

    @Test
    fun `an extension registered through mcp() is served`() {
        val server = mcp(
            ServerMetaData("test-server", "1.0.0"),
            NoMcpSecurity,
            extensions = listOf(FakeExtension(claims = unrouted))
        )

        assertThat(server.http!!(request("server/discover")).bodyString(), containsSubstring("org.http4k/fake"))
        assertThat(server.http!!(request(unrouted.value)).bodyString(), containsSubstring(MARKER))
    }

    @Test
    fun `an extension registered through mcpHttpNonStreaming is served`() {
        val server = mcpHttpNonStreaming(
            ServerMetaData("test-server", "1.0.0"),
            NoMcpSecurity,
            extensions = listOf(FakeExtension())
        )

        assertThat(server(request("server/discover")).bodyString(), containsSubstring("org.http4k/fake"))
    }

    @Test
    fun `an unparseable request for a registered extension method is invalid-request, not method-not-found`() {
        val response = protocol(FakeExtension())(request("fake/do", body = """{"jsonrpc":"2.0","id":"1","method":"fake/do"}"""))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("-32600"))
    }

    @Test
    fun `the same request is method-not-found when the extension is not registered`() {
        val response = protocol()(request("fake/do", body = """{"jsonrpc":"2.0","id":"1","method":"fake/do"}"""))

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.bodyString(), containsSubstring("-32601"))
    }

    private fun request(method: String, body: String = validBody(method)) = Request(POST, "/mcp")
        .header("mcp-protocol-version", LATEST_VERSION.value)
        .header("mcp-method", method)
        .body(body)

    private fun validBody(method: String) =
        """{"jsonrpc":"2.0","id":"1","method":"$method","params":{""" +
            """"_meta":{"io.modelcontextprotocol/protocolVersion":"${LATEST_VERSION.value}",""" +
            """"io.modelcontextprotocol/clientCapabilities":{}}}}"""
}

private const val MARKER = "handled by the fake extension"

private class FakeExtension(claims: McpRpcMethod = McpRpcMethod.of("fake/do")) : McpServerExtension {
    override val name = "org.http4k/fake"
    override val config = mapOf("greeting" to "hello")
    override val methods = setOf(claims)
    override fun invoke(mcp: McpRequest) =
        Ok(McpJsonRpcErrorResponse(mcp.message.id, ErrorMessage(-32099, MARKER)))
}
