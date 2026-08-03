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
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.cancellations
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.routing.bind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// Tests requestState integrity where it actually lives — the RoutingMcpHandler, which signs the state on the way
// out and verifies+decodes it on the way in. No HTTP transport involved (that's serve()'s concern).
class RequestStateIntegrityTest {

    private val codec = RequestStateCodec.Hmac("server-key".toByteArray())

    private var stateSeenByHandler: String? = null
    private var handlerCompleted = false

    private val handler = RoutingMcpHandler(
        discover = { McpDiscover.Response.Result(emptyList()) },
        completions = completions(),
        prompts = prompts(),
        resources = resources(),
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
        cancellations = cancellations(),
        requestStateCodec = codec
    )

    private fun call(requestState: String? = null): McpResponse = handler(
        McpRequest(
            McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("confirm"), requestState = requestState), "1"),
            Request(POST, "/mcp")
        )
    )

    private fun McpResponse.result() = ((this as McpResponse.Ok).message as McpTool.Call.Response).result

    @Test
    fun `an outgoing requestState is signed`() {
        val out = call().result().requestState

        assertThat(out, present())
        assertThat(out, !equalTo("server-continuation"))
        assertThat(codec.verify(out!!), equalTo("server-continuation"))
    }

    @Test
    fun `a valid requestState round-trips as decoded plaintext to the handler`() {
        val signed = call().result().requestState!!

        call(requestState = signed)

        assertThat(handlerCompleted, equalTo(true))
        assertThat(stateSeenByHandler, equalTo("server-continuation"))
    }

    @Test
    fun `a tampered requestState is rejected -32602 and the handler never runs`() {
        val signed = call().result().requestState!!

        val e = assertThrows<McpException> { call(requestState = signed + "-TAMPERED") }

        assertThat(e.error.code, equalTo(-32602))
        assertThat(handlerCompleted, equalTo(false))
        assertThat(stateSeenByHandler, absent())
    }
}
