/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.junit.jupiter.api.Test

class ValidateStatelessRequestTest {

    private val supported = setOf(LATEST_VERSION)
    private val v = LATEST_VERSION.value

    private fun request(headerVersion: String? = v) =
        Request(POST, "/mcp").let { r -> headerVersion?.let { r.header("mcp-protocol-version", it) } ?: r }

    private fun body(meta: String?) =
        """{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{${meta ?: ""}}}"""

    private fun meta(version: String? = v, caps: Boolean = true, info: Boolean = false) = buildString {
        append(""""_meta":{""")
        val fields = buildList {
            version?.let { add(""""io.modelcontextprotocol/protocolVersion":"$it"""") }
            if (caps) add(""""io.modelcontextprotocol/clientCapabilities":{}""")
            if (info) add(""""io.modelcontextprotocol/clientInfo":{"name":"c","version":"1"}""")
        }
        append(fields.joinToString(","))
        append("}")
    }

    private fun McpJsonRpcErrorResponse?.code() =
        (this?.error as? MoshiObject)?.get("code")?.let { McpJson.integer(it).toInt() }

    @Test
    fun `a fully-described request is valid`() {
        assertThat(validateStatelessRequest(body(meta()), request(), supported), absent())
    }

    @Test
    fun `a request omitting clientInfo is still valid`() {
        assertThat(validateStatelessRequest(body(meta(info = false)), request(), supported), absent())
    }

    @Test
    fun `missing _meta is rejected -32602`() {
        assertThat(validateStatelessRequest(body(null), request(), supported).code(), equalTo(-32602))
    }

    @Test
    fun `missing protocolVersion is rejected -32602`() {
        assertThat(validateStatelessRequest(body(meta(version = null)), request(), supported).code(), equalTo(-32602))
    }

    @Test
    fun `missing clientCapabilities is rejected -32602`() {
        assertThat(validateStatelessRequest(body(meta(caps = false)), request(), supported).code(), equalTo(-32602))
    }

    @Test
    fun `an unsupported version is rejected -32022`() {
        assertThat(
            validateStatelessRequest(body(meta(version = "v999.0.0")), request("v999.0.0"), supported).code(),
            equalTo(-32022)
        )
    }

    @Test
    fun `a header that does not match _meta is rejected -32020 even for an unsupported version`() {
        assertThat(
            validateStatelessRequest(body(meta(version = "v999.0.0")), request(v), supported).code(),
            equalTo(-32020)
        )
    }
}
