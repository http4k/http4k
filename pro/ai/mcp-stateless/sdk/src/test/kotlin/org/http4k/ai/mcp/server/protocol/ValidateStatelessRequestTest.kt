/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.protocolVersion
import org.junit.jupiter.api.Test

class ValidateStatelessRequestTest {

    private val supported = setOf(LATEST_VERSION)
    private val v = LATEST_VERSION.value

    private fun meta(version: ProtocolVersion? = LATEST_VERSION, caps: ClientCapabilities? = ClientCapabilities()): Meta {
        var m = Meta.default
        version?.let { m = MetaKey.protocolVersion().toLens()(it, m) }
        caps?.let { m = MetaKey.clientCapabilities().toLens()(it, m) }
        return m
    }

    private fun message(meta: Meta = meta()) = McpTool.List.Request(McpTool.List.Request.Params(_meta = meta), "1")

    private fun request(headerVersion: String? = v) =
        Request(POST, "/mcp").let { r -> headerVersion?.let { r.header("mcp-protocol-version", it) } ?: r }

    private fun McpJsonRpcErrorResponse?.code() =
        (this?.error as? MoshiObject)?.get("code")?.let { McpJson.integer(it).toInt() }

    @Test
    fun `a fully-described request is valid`() {
        assertThat(validateStatelessRequest(message(), request(), supported), absent())
    }

    @Test
    fun `a request omitting clientInfo is still valid`() {
        assertThat(validateStatelessRequest(message(meta()), request(), supported), absent())
    }

    @Test
    fun `missing protocolVersion is rejected -32602`() {
        assertThat(validateStatelessRequest(message(meta(version = null)), request(), supported).code(), equalTo(-32602))
    }

    @Test
    fun `missing clientCapabilities is rejected -32602`() {
        assertThat(validateStatelessRequest(message(meta(caps = null)), request(), supported).code(), equalTo(-32602))
    }

    @Test
    fun `an unsupported version is rejected -32022`() {
        val v999 = ProtocolVersion.of("v999.0.0")
        assertThat(
            validateStatelessRequest(message(meta(version = v999)), request("v999.0.0"), supported).code(),
            equalTo(-32022)
        )
    }

    @Test
    fun `a header that does not match _meta is rejected -32020 even for an unsupported version`() {
        val v999 = ProtocolVersion.of("v999.0.0")
        assertThat(
            validateStatelessRequest(message(meta(version = v999)), request(v), supported).code(),
            equalTo(-32020)
        )
    }
}
