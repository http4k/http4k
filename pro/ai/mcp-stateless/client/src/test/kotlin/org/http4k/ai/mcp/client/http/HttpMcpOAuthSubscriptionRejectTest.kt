/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.OAuthMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

class HttpMcpOAuthSubscriptionRejectTest : PortBasedTest {

    private val server = mcp(
        ServerMetaData(McpEntity.of("secure"), Version.of("1.0.0")),
        OAuthMcpSecurity(Uri.of("http://auth"), Uri.of("http://mcp/mcp")) { it == "good" },
        Tool("greet", "greets") bind { ToolResponse.Ok("hi") }
    ).asServer(Helidon(0)).start()

    private val http = JavaHttpClient()

    private fun openSubscription(token: String?) = http(
        Request(POST, "http://localhost:${server.port()}/mcp")
            .header("Accept", TEXT_EVENT_STREAM.value)
            .header("mcp-method", "subscriptions/listen")
            .let { if (token != null) it.header("Authorization", "Bearer $token") else it }
            .body("""{"jsonrpc":"2.0","id":"1","method":"subscriptions/listen","params":{"notifications":{}}}""")
    )

    @Test
    fun `a subscription open with no bearer is cleanly rejected with 401`() {
        val response = openSubscription(null)

        assertThat(response.status, equalTo(UNAUTHORIZED))
        assertThat(response.header("WWW-Authenticate"), present())
    }

    @Test
    fun `a subscription open with a bad bearer is cleanly rejected with 401`() {
        assertThat(openSubscription("bad").status, equalTo(UNAUTHORIZED))
    }
}
