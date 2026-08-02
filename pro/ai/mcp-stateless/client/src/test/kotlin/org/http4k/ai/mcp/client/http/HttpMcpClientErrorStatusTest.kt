/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Failure
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.model.ToolName
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class HttpMcpClientErrorStatusTest {

    // 2026-07-28 returns validation/method errors as 4xx; the body is still a JSON-RPC error.
    private val http: HttpHandler = {
        Response(BAD_REQUEST)
            .header("content-type", "application/json")
            .body("""{"jsonrpc":"2.0","id":"1","error":{"code":-32602,"message":"Invalid params"}}""")
    }

    private val client: McpClient = HttpMcpClient(Uri.of("/mcp"), http = http)

    @Test
    fun `a 4xx JSON-RPC error surfaces as a Protocol error, not a transport error`() {
        val result = client.tools().call(ToolName.of("greet"), ToolRequest())

        val reason = (result as Failure).reason
        assertThat(reason, present(isA<McpError.Protocol>()))
        assertThat((reason as McpError.Protocol).error.code, equalTo(-32602))
    }
}
