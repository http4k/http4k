/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.DRAFT
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.junit.jupiter.api.Test

class ValidateMcpMethodHeaderTest {

    private val session = Session(SessionId.of("test"))
    private val ok = McpResponse.Accepted

    private fun clientTracking(protocolVersion: org.http4k.ai.mcp.protocol.ProtocolVersion) =
        mutableMapOf(
            session to ClientTracking(
                McpInitialize.Request.Params(
                    VersionedMcpEntity(McpEntity.of("test"), Version.of("1")),
                    ClientCapabilities(),
                    protocolVersion
                )
            )
        )

    private fun mcpRequest(mcpMethodHeader: McpRpcMethod? = null) = McpRequest(
        session,
        McpPing.Request(id = 1),
        Request(POST, "/mcp").let { req ->
            mcpMethodHeader?.let { req.with(Header.MCP_METHOD of it) } ?: req
        }
    )

    @Test
    fun `passes through when no header present`() {
        val filter = ValidateMcpMethodHeader(clientTracking(DRAFT))
        val result = filter.then { ok }(mcpRequest())
        assertThat(result, equalTo(ok))
    }

    @Test
    fun `passes through when header matches method`() {
        val filter = ValidateMcpMethodHeader(clientTracking(DRAFT))
        val result = filter.then { ok }(mcpRequest(McpRpcMethod.of("ping")))
        assertThat(result, equalTo(ok))
    }

    @Test
    fun `rejects when header does not match method`() {
        val filter = ValidateMcpMethodHeader(clientTracking(DRAFT))
        val result = filter.then { ok }(mcpRequest(McpRpcMethod.of("tools/call")))
        assertThat(result, isA<McpResponse.Ok>())
    }

    @Test
    fun `skips validation for non-draft protocol`() {
        val filter = ValidateMcpMethodHeader(clientTracking(LATEST_VERSION))
        val result = filter.then { ok }(mcpRequest(McpRpcMethod.of("tools/call")))
        assertThat(result, equalTo(ok))
    }

    @Test
    fun `skips validation when no client tracking`() {
        val filter = ValidateMcpMethodHeader(emptyMap())
        val result = filter.then { ok }(mcpRequest(McpRpcMethod.of("tools/call")))
        assertThat(result, equalTo(ok))
    }
}
