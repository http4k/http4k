/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.junit.jupiter.api.Test

class ValidateMcpMethodHeaderTest {

    private val ok = McpResponse.Accepted

    private fun mcpRequest(mcpMethodHeader: McpRpcMethod? = null) = McpRequest(
        McpTool.List.Request(id = 1),
        Request(POST, "/mcp").let { req -> mcpMethodHeader?.let { req.with(Header.MCP_METHOD of it) } ?: req }
    )

    @Test
    fun `rejects when header absent (mirror header is required)`() {
        assertThat(ValidateMcpMethodHeader().then { ok }(mcpRequest()), isA<McpResponse.Ok>())
    }

    @Test
    fun `passes through when header matches method`() {
        assertThat(ValidateMcpMethodHeader().then { ok }(mcpRequest(McpRpcMethod.of("tools/list"))), equalTo(ok))
    }

    @Test
    fun `rejects when header does not match method`() {
        assertThat(ValidateMcpMethodHeader().then { ok }(mcpRequest(McpRpcMethod.of("tools/call"))), isA<McpResponse.Ok>())
    }
}
