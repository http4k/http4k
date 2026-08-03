/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class RequestStateIntegrityTest {

    private val codec = RequestStateCodec.Hmac("server-key".toByteArray())

    private var stateSeenByHandler: String? = null
    private var handlerCompleted = false

    private val protocol = McpProtocol(
        VersionedMcpEntity(McpEntity.of("t"), Version.of("1.0.0")),
        tools = tools(
            Tool("confirm", "confirms") bind { req ->
                if (req.requestState == null) {
                    ToolResponse.InputRequired(emptyMap(), requestState = "server-continuation")
                } else {
                    stateSeenByHandler = req.requestState
                    handlerCompleted = true
                    ToolResponse.Ok("done")
                }
            }
        ),
        supportedVersions = setOf(LATEST_VERSION),
        requestStateCodec = codec
    )

    private fun toolCall(requestState: String? = null) = Request(POST, "/mcp")
        .header("mcp-protocol-version", LATEST_VERSION.value)
        .header("mcp-method", "tools/call")
        .header("mcp-name", "confirm")
        .body(
            """{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"confirm",""" +
                (requestState?.let { """"requestState":"$it",""" } ?: "") +
                """"_meta":{"io.modelcontextprotocol/protocolVersion":"${LATEST_VERSION.value}",""" +
                """"io.modelcontextprotocol/clientCapabilities":{}}}}"""
        )

    private fun McpResponse.result() = ((this as McpResponse.Ok).message as McpTool.Call.Response).result
    private fun McpResponse.errorCode() =
        (((this as McpResponse.Ok).message as McpJsonRpcErrorResponse).error as MoshiObject)["code"]
            ?.let { McpJson.integer(it).toInt() }

    @Test
    fun `an outgoing requestState is signed`() {
        val out = protocol.receive(toolCall()).result().requestState

        assertThat(out, present())
        assertThat(out, !equalTo("server-continuation"))
        assertThat(codec.verify(out!!), equalTo("server-continuation"))
    }

    @Test
    fun `a valid requestState round-trips as decoded plaintext to the handler`() {
        val signed = protocol.receive(toolCall()).result().requestState!!

        val response = protocol.receive(toolCall(requestState = signed))

        assertThat(handlerCompleted, equalTo(true))
        assertThat(stateSeenByHandler, equalTo("server-continuation"))
        assertThat(response.result().content?.firstOrNull()?.toString(), present())
    }

    @Test
    fun `a tampered requestState is rejected -32602 and the handler never runs`() {
        val signed = protocol.receive(toolCall()).result().requestState!!

        val response = protocol.receive(toolCall(requestState = signed + "-TAMPERED"))

        assertThat(response.errorCode(), equalTo(-32602))
        assertThat(handlerCompleted, equalTo(false))
        assertThat(stateSeenByHandler, absent())
    }
}
