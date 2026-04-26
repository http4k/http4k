/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.acceptance

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.mcp.testing.useClient
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.util.FixedClock
import org.http4k.util.PortBasedTest
import org.http4k.wiretap.Wiretap
import org.http4k.wiretap.WiretapTarget
import org.http4k.wiretap.util.Json
import org.junit.jupiter.api.Test

interface WiretapSmokeContract : PortBasedTest {

    val target: WiretapTarget
    val testRequest: Request

    @Test
    fun `can boot and count tools`() {
        Wiretap(target).testMcpClient(Request(POST, "_wiretap/mcp")).useClient {
            assertThat(tools().list().coerce<List<McpTool>>().size, equalTo(18))
        }
    }

    @Test
    fun `transactions through wiretap are stored`() {
        val wiretap = Wiretap(target, clock = FixedClock)

        wiretap.testMcpClient(Request(POST, "_wiretap/mcp")).useClient {
            wiretap.http!!(testRequest)

            val call = tools().call(ToolName.of("list_transactions")).coerce<ToolResponse.Ok>()

            val calls = call.content!![0] as Text
            val elements = Json.elements(Json.parse(calls.text))
            assertThat(elements.size, greaterThan(0))
        }
    }
}
