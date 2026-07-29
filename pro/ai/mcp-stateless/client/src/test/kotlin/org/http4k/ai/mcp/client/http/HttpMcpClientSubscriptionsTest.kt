/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS

class HttpMcpClientSubscriptionsTest : PortBasedTest {

    private val tools = tools(Tool("greet", "greets") bind { ToolResponse.Ok("hi") })
    private val resources = resources()
    private val server = HttpMcp(
        McpProtocol(VersionedMcpEntity("subs-server", "1.0.0"), tools = tools, resources = resources), NoMcpSecurity
    ).asServer(Helidon(0)).start()

    private fun client() = HttpMcpClient(Uri.of("http://localhost:${server.port()}/mcp"))

    // the stream + server-side observer register asynchronously after listen(); nudge until the handler fires
    private fun awaitFiring(latch: CountDownLatch, trigger: () -> Unit): Boolean {
        repeat(50) {
            trigger()
            if (latch.await(100, MILLISECONDS)) return true
        }
        return false
    }

    @Test
    fun `a tools list change fires the subscribed handler`() {
        val client = client()
        val fired = CountDownLatch(1)

        val sub = client.tools().onListChanged { fired.countDown() }.valueOrNull()!!
        try {
            assertThat(awaitFiring(fired) { tools.items = tools.items.toList() }, equalTo(true))
        } finally {
            sub.close()
            client.close()
        }
    }

    @Test
    fun `a resource update fires the handler for that URI only`() {
        val client = client()
        val watched = CountDownLatch(1)
        val ignored = CountDownLatch(1)

        val sub = client.resources().subscribe(Uri.of("res://watched")) { watched.countDown() }.valueOrNull()!!
        try {
            assertThat(
                awaitFiring(watched) {
                    resources.triggerUpdated(Uri.of("res://ignored")) // no handler for this URI
                    resources.triggerUpdated(Uri.of("res://watched"))
                },
                equalTo(true)
            )
            assertThat(ignored.await(200, MILLISECONDS), equalTo(false))
        } finally {
            sub.close()
            client.close()
        }
    }
}
