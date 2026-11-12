/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import org.http4k.core.PolyHandler
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer

class Node(val handler: PolyHandler, private vararg val parts: AutoCloseable) : Lifecycle {
    override fun start() = parts.filterIsInstance<Lifecycle>().forEach(Lifecycle::start)

    override fun close() = parts.reversed().forEach { runCatching { it.close() } }

    fun asServer(config: PolyServerConfig): Http4kServer = object : Http4kServer {
        private val server = handler.asServer(config)

        override fun start() = apply {
            this@Node.start()
            server.start()
        }

        override fun stop() = apply {
            server.stop()
            this@Node.close()
        }

        override fun port() = server.port()
    }
}
