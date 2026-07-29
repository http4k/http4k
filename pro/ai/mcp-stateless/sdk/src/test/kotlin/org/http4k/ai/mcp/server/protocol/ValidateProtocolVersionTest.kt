/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.junit.jupiter.api.Test

class ValidateProtocolVersionTest {

    private val supported = setOf(ProtocolVersion.of("2026-07-28"))
    private val ok = McpResponse.Accepted

    private fun request(version: ProtocolVersion) = McpRequest(
        McpTool.List.Request(id = 1),
        Request(POST, "/mcp").with(Header.MCP_PROTOCOL_VERSION of version)
    )

    @Test
    fun `passes through a supported version`() {
        val result = ValidateProtocolVersion(supported).then { ok }(request(ProtocolVersion.of("2026-07-28")))
        assertThat(result, equalTo(ok))
    }

    @Test
    fun `rejects an unsupported version with -32022 including requested and supported`() {
        val result = ValidateProtocolVersion(supported).then { ok }(request(ProtocolVersion.of("1900-01-01")))

        val error = ((result as McpResponse.Ok).message as McpJsonRpcErrorResponse).error
        val json = McpJson.compact(error)
        assertThat(json, containsSubstring("-32022"))
        assertThat(json, containsSubstring("1900-01-01"))
        assertThat(json, containsSubstring("2026-07-28"))
    }
}
