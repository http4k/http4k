/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.fanbelt.eventually
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

abstract class TransportContract {
    abstract fun transport(): Transport

    abstract fun dropConnections()

    private val opened = CopyOnWriteArrayList<Transport>()
    private val topic = TenantId.of("t-${System.nanoTime()}")(Topic.of("t"))
    private val heard = CopyOnWriteArrayList<Topic>()
    private val reconnects = AtomicInteger()

    private fun started(received: (Topic) -> Unit = {}, reconnected: () -> Unit = {}) = transport().apply {
        start(received, reconnected)
        opened += this
    }

    private fun listener() = started({ heard += it }, { reconnects.incrementAndGet() }).apply { interested(Interest.of(topic)) }

    private fun Transport.reaches() {
        val sent = Topic.of("${topic.value}/${UUID.randomUUID()}")
        eventually {
            send(sent)
            Thread.sleep(20)
            sent in heard
        }
    }

    @AfterEach
    fun close() = opened.forEach(Transport::close)

    @Test
    fun `a message reaches another node interested in it`() {
        listener()

        started().reaches()
    }

    @Test
    fun `a node hears its own message`() {
        listener().reaches()
    }

    @Test
    fun `a node that loses its connection says it has reconnected, and hears again`() {
        listener()
        val sender = started()
        sender.reaches()
        val before = reconnects.get()

        dropConnections()

        eventually { reconnects.get() > before }
        sender.reaches()
    }
}
