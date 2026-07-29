/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.McpEntity
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.fromHeader
import org.http4k.ai.mcp.stateless.model.string
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.Version
import org.http4k.ai.mcp.stateless.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.MoshiObject
import org.http4k.routing.stateless.bind
import org.http4k.routing.stateless.mcp
import org.junit.jupiter.api.Test

class McpParamMirrorTest {

    private val server = mcp(
        ServerMetaData(McpEntity.of("ExampleServer"), Version.of("1.0.0")),
        NoMcpSecurity,
        Tool(
            "search", "searches",
            Tool.Arg.string().required("region", "the region", fromHeader("Region"))
        ) bind { ToolResponse.Ok("ok") }
    )

    private fun call(bodyValue: String, headerValue: String?) = server.http!!(
        Request(POST, "/mcp")
            .header("Accept", APPLICATION_JSON.value)
            .header("mcp-protocol-version", LATEST_VERSION.value)
            .header("mcp-method", "tools/call")
            .header("mcp-name", "search")
            .let { if (headerValue == null) it else it.header("Mcp-Param-Region", headerValue) }
            .body(
                """{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"search",""" +
                    """"arguments":{"region":${McpJson.asFormatString(bodyValue)}},""" +
                    """"_meta":{"io.modelcontextprotocol/protocolVersion":"${LATEST_VERSION.value}",""" +
                    """"io.modelcontextprotocol/clientCapabilities":{}}}}"""
            )
    )

    private fun errorCodeOf(body: String) =
        ((McpJson.parse(body) as MoshiObject).attributes["error"] as MoshiObject)
            .attributes["code"]?.let { McpJson.integer(it).toInt() }

    @Test
    fun `a matching base64 sentinel is accepted`() {
        assertThat(call("Hello", "=?base64?SGVsbG8=?=").status, equalTo(OK))
    }

    @Test
    fun `a matching literal is accepted`() {
        assertThat(call("eu-west", "eu-west").status, equalTo(OK))
    }

    @Test
    fun `a literal that is missing the sentinel prefix is not decoded`() {
        assertThat(call("SGVsbG8=", "SGVsbG8=").status, equalTo(OK))
    }

    @Test
    fun `a literal that is missing the sentinel suffix is not decoded`() {
        assertThat(call("=?base64?SGVsbG8=", "=?base64?SGVsbG8=").status, equalTo(OK))
    }

    @Test
    fun `a malformed sentinel payload is rejected`() {
        val badPadding = call("Hello", "=?base64?SGVsbG8?=")
        assertThat(badPadding.status, equalTo(BAD_REQUEST))
        assertThat(errorCodeOf(badPadding.bodyString()), equalTo(HeaderMismatchError.CODE))

        assertThat(call("Hello", "=?base64?SGVs!!!bG8=?=").status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `a mismatched value is rejected`() {
        val response = call("eu-west", "us-east")
        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(errorCodeOf(response.bodyString()), equalTo(HeaderMismatchError.CODE))
    }

    @Test
    fun `an omitted header with the value present in the body is rejected`() {
        val response = call("eu-west", null)
        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(errorCodeOf(response.bodyString()), equalTo(HeaderMismatchError.CODE))
    }
}
