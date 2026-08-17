/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

/**
 * The acceptance gate for the extension seam: everything here uses a fake, non-task extension. If any of
 * it needs task-shaped machinery, the seam is not generic.
 */
class McpExtensionRegistrationTest {

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
    fun `a registered extension's filter intercepts the methods it claims`() {
        val response = protocol(FakeExtension(intercepts = McpRpcMethod.of("tools/list")))(request("tools/list"))

        assertThat(response.status, equalTo(ACCEPTED))
    }

    @Test
    fun `a registered extension passes other methods through`() {
        val response = protocol(FakeExtension(intercepts = McpRpcMethod.of("tools/list")))(request("server/discover"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("supportedVersions"))
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

private class FakeExtension(private val intercepts: McpRpcMethod = McpRpcMethod.of("fake/do")) : McpServerExtension {
    override val name = "org.http4k/fake"
    override val config = mapOf("greeting" to "hello")
    override val methods = setOf(McpRpcMethod.of("fake/do"))
    override val filter = McpFilter { next ->
        { mcp -> if (mcp.message.method == intercepts) Accepted else next(mcp) }
    }
}
