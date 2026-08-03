/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class ToHttpRequestTest {

    private fun httpFor(message: McpJsonRpcRequest) =
        message.toHttpRequest(LATEST_VERSION, Uri.of("/mcp"), VersionedMcpEntity(McpEntity.of("c"), Version.of("1")), ClientCapabilities())

    @Test
    fun `stamps the Mcp-Name mirror header for a targeted request`() {
        val http = httpFor(McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("greet")), "1"))

        assertThat(http.header("mcp-name"), equalTo("greet"))
    }

    @Test
    fun `omits Mcp-Name for a non-targeted request`() {
        val http = httpFor(McpTool.List.Request(id = "1"))

        assertThat(http.header("mcp-name"), absent())
    }
}
