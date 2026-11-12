/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NodeTest {
    private val log = mutableListOf<String>()

    private fun part(name: String) = object : Lifecycle {
        override fun start() {
            log += "start $name"
        }

        override fun close() {
            log += "close $name"
        }
    }

    private val server = object : PolyServerConfig {
        override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?) = object : Http4kServer {
            override fun start() = apply { log += "start server" }
            override fun stop() = apply { log += "stop server" }
            override fun port() = 0
        }
    }

    @Test
    fun `parts start in order before the server, and close in reverse after it stops`() {
        val running = Node(PolyHandler(), AutoCloseable { log += "close scheduler" }, part("bus"), part("presence"), part("jobs"))
            .asServer(server)
            .start()

        running.stop()

        assertEquals(
            listOf(
                "start bus", "start presence", "start jobs", "start server",
                "stop server", "close jobs", "close presence", "close bus", "close scheduler",
            ),
            log,
        )
    }

    @Test
    fun `one part failing to close doesn't stop the rest closing`() {
        Node(PolyHandler(), part("first"), AutoCloseable { error("boom") }, part("last")).close()

        assertEquals(listOf("close last", "close first"), log)
    }
}
