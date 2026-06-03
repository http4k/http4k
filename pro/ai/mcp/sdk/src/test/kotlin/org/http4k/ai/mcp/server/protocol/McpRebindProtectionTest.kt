/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpNonStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.jsonrpc.JsonRpcMcp
import org.http4k.ai.mcp.server.jsonrpc.JsonRpcSessions
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.ai.mcp.server.websocket.WebsocketMcp
import org.http4k.ai.mcp.server.websocket.WebsocketSessions
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.websocket.WsStatus
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
    fun `JsonRpcMcp - preflight from disallowed origin omits ACAO`() {
        val mcp = JsonRpcMcp(
            McpProtocol(metadata, JsonRpcSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val response = mcp(Request(OPTIONS, "/jsonrpc").header("Origin", "http://evil.example"))

        assertThat(response.header("access-control-allow-origin"), absent())
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

    @Test
    fun `WebsocketMcp - upgrade with disallowed origin is refused`() {
        val mcp = WebsocketMcp(
            McpProtocol(metadata, WebsocketSessions()),
            NoMcpSecurity,
            corsPolicy = policy
        )

        val refusalCalls = mutableListOf<WsStatus>()
        val fakeWs = object : org.http4k.websocket.Websocket {
            override fun send(message: org.http4k.websocket.WsMessage) {}
            override fun close(status: WsStatus) {
                refusalCalls += status
            }

            override fun onError(fn: (Throwable) -> Unit) {}
            override fun onClose(fn: (WsStatus) -> Unit) {}
            override fun onMessage(fn: (org.http4k.websocket.WsMessage) -> Unit) {}
        }

        val wsResponse = mcp.ws!!(Request(GET, "/").header("Origin", "http://evil.example"))
        wsResponse.consumer(fakeWs)

        assertThat(refusalCalls.firstOrNull(), present())
    }

}
