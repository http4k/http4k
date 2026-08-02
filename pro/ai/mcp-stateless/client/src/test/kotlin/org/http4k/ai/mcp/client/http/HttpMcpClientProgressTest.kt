/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.LogLevel.info
import org.http4k.ai.mcp.model.LogMessage
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

class HttpMcpClientProgressTest : PortBasedTest {

    private val tools = tools(
        Tool("greet", "greets") bind { req ->
            req.client.progress("t", 1, 2.0, "step1")
            req.client.progress("t", 2, 2.0, "step2")
            req.client.log("hello", info)
            ToolResponse.Ok("done")
        }
    )
    private val server = HttpMcp(McpProtocol(VersionedMcpEntity("streamer", "1.0.0"), tools = tools), NoMcpSecurity)
        .asServer(Helidon(0)).start()

    private fun client() = HttpMcpClient(Uri.of("http://localhost:${server.port()}/mcp"))

    @Test
    fun `progress and log stream to the per-call callbacks, then the result returns`() {
        val client = client()
        val progresses = mutableListOf<Progress>()
        val logs = mutableListOf<LogMessage>()
        try {
            val result = client.tools().call(
                ToolName.of("greet"),
                onProgress = { progresses += it },
                onLog = { logs += it }
            )

            assertThat((result.valueOrNull() as ToolResponse.Ok).content, equalTo(listOf(Text("done"))))
            assertThat(progresses.map { it.progress }, equalTo(listOf(1, 2)))
            assertThat(progresses.last().total, equalTo(2.0))
            assertThat(logs.map { it.level }, equalTo(listOf(info)))
        } finally {
            client.close()
        }
    }

    @Test
    fun `without callbacks the call is a plain single-JSON response`() {
        val client = client()
        try {
            assertThat(
                (client.tools().call(ToolName.of("greet")).valueOrNull() as ToolResponse.Ok).content,
                equalTo(listOf(Text("done")))
            )
        } finally {
            client.close()
        }
    }
}
