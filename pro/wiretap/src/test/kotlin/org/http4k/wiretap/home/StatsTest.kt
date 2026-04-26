/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.home

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.model.ToolName
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.mcp
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.util.Metrics
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test

class StatsTest {

    private val templates = Templates()
    private val renderer = DatastarElementRenderer(templates)
    private val meterRegistry = Metrics()

    private val function = GetStats(
        trafficMetrics = TrafficMetrics(meterRegistry),
        traceStore = TraceStore.InMemory(),
        inboundChaos = ChaosEngine(),
        outboundChaos = ChaosEngine(),
        mcpCapabilities = McpCapabilities("none"),
        meterRegistry = meterRegistry
    )

    @Test
    fun `stats include uptime in HTTP response`() {
        val response = function.http(renderer, templates)(Request(GET, "/stats"))
        val body = response.bodyString()
        assertThat(response.status, equalTo(OK))
        assertThat(body, containsSubstring("Uptime"))
    }

    @Test
    fun `stats include JVM metrics in HTTP response`() {
        val response = function.http(renderer, templates)(Request(GET, "/stats"))

        assertThat(response.status, equalTo(OK))
        val body = response.bodyString()
        assertThat(body, containsSubstring("Heap Memory"))
        assertThat(body, containsSubstring("Threads"))
    }

    @Test
    fun `stats include JVM heap values in response`() {
        val response = function.http(renderer, templates)(Request(GET, "/stats"))
        val body = response.bodyString()
        assertThat(body, containsSubstring("MB"))
        assertThat(body, containsSubstring("Process CPU"))
        assertThat(body, containsSubstring("GC Pauses"))
    }

    @Test
    fun `stats include class loader metrics in response`() {
        val response = function.http(renderer, templates)(Request(GET, "/stats"))
        val body = response.bodyString()
        assertThat(body, containsSubstring("Classes"))
    }

    @Test
    fun `mcp returns stats as JSON`() {
        val client = mcp(
            ServerMetaData("entity", "version"),
            NoMcpSecurity,
            function.mcp()
        ).testMcpClient(Request(GET, "/mcp"))

        val result = client.tools().call(
            ToolName.of("get_stats"),
            ToolRequest(emptyMap())
        ).coerce<Ok>()

        val content = result.content!!.first().toString()
        assertThat(content, containsSubstring("uptime"))
        assertThat(content, containsSubstring("heapUsedMb"))
        assertThat(content, containsSubstring("threadCount"))
    }
}
