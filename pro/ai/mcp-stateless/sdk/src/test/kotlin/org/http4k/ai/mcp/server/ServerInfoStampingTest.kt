/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.http4k.lens.MetaKey
import org.http4k.lens.serverInfo
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.junit.jupiter.api.Test

// serverInfo is stamped into every result's _meta at construction (RoutingMcpHandler), not in the HTTP layer.
class ServerInfoStampingTest {

    private val serverInfo = VersionedMcpEntity(McpEntity.of("ExampleServer"), Version.of("1.0.0"))
    private val server = mcp(
        ServerMetaData(McpEntity.of("ExampleServer"), Version.of("1.0.0")),
        NoMcpSecurity,
        Tool("greet", "greets") bind { ToolResponse.Ok("hello") }
    )

    private fun call(headerMethod: String, bodyMethod: String) = server.http!!(
        Request(POST, "/mcp")
            .header("Accept", APPLICATION_JSON.value)
            .header("mcp-protocol-version", LATEST_VERSION.value)
            .header("mcp-method", headerMethod)
            .header("mcp-name", "greet")
            .body(
                """{"jsonrpc":"2.0","id":"1","method":"$bodyMethod","params":{"name":"greet",""" +
                    """"_meta":{"io.modelcontextprotocol/protocolVersion":"${LATEST_VERSION.value}",""" +
                    """"io.modelcontextprotocol/clientCapabilities":{}}}}"""
            )
    )

    @Test
    fun `a result response carries serverInfo in its _meta`() {
        val body = McpJson.parse(call("tools/call", "tools/call").bodyString()) as MoshiObject
        val resultMeta = Meta(body.result().attributes["_meta"] as MoshiObject)

        assertThat(MetaKey.serverInfo().toLens()(resultMeta), equalTo(serverInfo))
    }

    @Test
    fun `an error response has no result and so no serverInfo`() {
        // Mcp-Method header != body method -> -32020 error response (no result)
        val body = McpJson.parse(call("prompts/list", "tools/call").bodyString()) as MoshiObject

        assertThat(body.attributes["result"], absent())
        assertThat(body.attributes["error"], present())
    }

    private fun MoshiObject.result() = attributes["result"] as MoshiObject
}
