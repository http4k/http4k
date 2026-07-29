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
import org.http4k.ai.mcp.server.http.HttpNonStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.junit.jupiter.api.Test

class McpRebindProtectionTest {

    private val metadata = ServerMetaData(McpEntity.of("server"), Version.of("1"))

    private val policy = CorsPolicy(
        OriginPolicy.AnyOf("http://localhost:4000"),
        listOf("content-type", "mcp-session-id"),
        listOf(GET, POST, DELETE)
    )

    @Test
    fun `HttpNonStreamingMcp - preflight from disallowed origin omits ACAO`() {
        val mcp = HttpNonStreamingMcp(
            McpProtocol(metadata, HttpSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp(Request(OPTIONS, "/mcp").header("Origin", "http://evil.example"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("access-control-allow-origin"), absent())
    }

    @Test
    fun `HttpNonStreamingMcp - POST from disallowed origin is forbidden`() {
        val mcp = HttpNonStreamingMcp(
            McpProtocol(metadata, HttpSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp(Request(POST, "/mcp").header("Origin", "http://evil.example").body("{}"))

        assertThat(response.status, equalTo(FORBIDDEN))
    }

    @Test
    fun `HttpStreamingMcp - POST from disallowed origin is forbidden`() {
        val mcp = HttpStreamingMcp(
            McpProtocol(metadata, HttpSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp.http!!(Request(POST, "/mcp").header("Origin", "http://evil.example").body("{}"))

        assertThat(response.status, equalTo(FORBIDDEN))
    }

    @Test
    fun `HttpNonStreamingMcp - preflight from allowed origin returns ACAO`() {
        val mcp = HttpNonStreamingMcp(
            McpProtocol(metadata, HttpSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp(Request(OPTIONS, "/mcp").header("Origin", "http://localhost:4000"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("access-control-allow-origin"), equalTo("http://localhost:4000"))
    }

    @Test
    fun `HttpStreamingMcp - preflight on HTTP side from disallowed origin omits ACAO`() {
        val mcp = HttpStreamingMcp(
            McpProtocol(metadata, HttpSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp.http!!(Request(OPTIONS, "/mcp").header("Origin", "http://evil.example"))

        assertThat(response.header("access-control-allow-origin"), absent())
    }

    @Test
    fun `SseMcp - preflight on HTTP side from disallowed origin omits ACAO`() {
        val mcp = SseMcp(
            McpProtocol(metadata, SseSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp.http!!(Request(OPTIONS, "/messages").header("Origin", "http://evil.example"))

        assertThat(response.header("access-control-allow-origin"), absent())
    }
}
