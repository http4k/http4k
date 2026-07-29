/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.junit.jupiter.api.Test

class McpRebindProtectionTest {

    private val metadata = ServerMetaData(McpEntity.of("server"), Version.of("1"))

    private val policy = CorsPolicy(
        OriginPolicy.AnyOf("http://localhost:4000"),
        listOf("content-type"),
        listOf(POST)
    )

    private fun server() = HttpMcp(McpProtocol(metadata), NoMcpSecurity, corsPolicy = policy).http!!

    @Test
    fun `preflight from disallowed origin omits ACAO`() {
        val response = server()(Request(OPTIONS, "/mcp").header("Origin", "http://evil.example"))
        assertThat(response.header("access-control-allow-origin"), absent())
    }

    @Test
    fun `POST from disallowed origin is forbidden`() {
        val response = server()(Request(POST, "/mcp").header("Origin", "http://evil.example").body("{}"))
        assertThat(response.status, equalTo(FORBIDDEN))
    }

    @Test
    fun `preflight from allowed origin returns ACAO`() {
        val response = server()(Request(OPTIONS, "/mcp").header("Origin", "http://localhost:4000"))
        assertThat(response.header("access-control-allow-origin"), equalTo("http://localhost:4000"))
    }
}
