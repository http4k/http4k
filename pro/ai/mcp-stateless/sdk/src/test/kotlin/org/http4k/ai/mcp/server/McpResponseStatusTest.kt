/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.ai.mcp.protocol.messages.UnsupportedProtocolVersionError
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.asHttp
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.junit.jupiter.api.Test

class McpResponseStatusTest {

    private fun statusFor(error: ErrorMessage) =
        McpResponse.Ok(McpJsonRpcErrorResponse("1", error)).asHttp().status

    @Test
    fun `method-not-found maps to 404`() {
        assertThat(statusFor(MethodNotFound), equalTo(NOT_FOUND))
    }

    @Test
    fun `invalid-params and the reserved MCP validation errors map to 400`() {
        assertThat(statusFor(InvalidParams), equalTo(BAD_REQUEST))
        assertThat(statusFor(HeaderMismatchError("mismatch")), equalTo(BAD_REQUEST))
        assertThat(statusFor(MissingRequiredClientCapabilityError(listOf("sampling"))), equalTo(BAD_REQUEST))
        assertThat(
            statusFor(UnsupportedProtocolVersionError(LATEST_VERSION, listOf(LATEST_VERSION))),
            equalTo(BAD_REQUEST)
        )
    }

    @Test
    fun `internal errors and domain results stay 200`() {
        assertThat(statusFor(InternalError), equalTo(OK))
    }

    @Test
    fun `a normal result is 200`() {
        val response = McpResponse.Ok(McpDiscover.Response(McpDiscover.Response.Result(listOf(LATEST_VERSION)), "1"))
        assertThat(response.asHttp().status, equalTo(OK))
    }
}
