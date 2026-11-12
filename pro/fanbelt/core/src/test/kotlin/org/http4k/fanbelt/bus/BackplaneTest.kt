/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.events.Event
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class BackplaneTest {
    private val scheduler = DeterministicScheduler(Instant.parse("2026-01-01T09:00:00Z"))
    private val events = CopyOnWriteArrayList<Event>()
    private val topic = Topic.of("t")
    private val broker = InMemoryBroker()

    private fun node(transport: Transport = broker.transport()) = Backplane(transport, scheduler, { events += it }, Duration.ofMinutes(5))

    private fun Bus.changes(): AtomicInteger {
        val count = AtomicInteger()
        subscribe(setOf(topic)) { count.incrementAndGet() }
        return count
    }

    private fun settle() = scheduler.runUntilIdle()

    @Test
    fun `a publish reaches subscribers on every started node, and the sender's own once`() {
        val a = node().apply { start() }
        val b = node().apply { start() }
        val onA = a.changes()
        val onB = b.changes()

        a.publish(topic)
        settle()

        assertEquals(1, onA.get())
        assertEquals(1, onB.get())
    }

    @Test
    fun `a node that isn't started, or has closed, hears nothing from the others`() {
        val a = node().apply { start() }
        val idle = node()
        val closed = node().apply { start() }.apply { close() }
        val onIdle = idle.changes()
        val onClosed = closed.changes()

        a.publish(topic)
        settle()

        assertEquals(0, onIdle.get())
        assertEquals(0, onClosed.get())
    }

    @Test
    fun `reconnecting marks every local subscriber dirty, because messages may have been missed`() {
        val reconnect = CopyOnWriteArrayList<() -> Unit>()
        val transport = object : Transport {
            override fun send(topic: Topic) {}
            override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {
                reconnect += reconnected
            }
            override fun close() {}
        }
        val bus = node(transport).apply { start() }
        val changes = bus.changes()

        reconnect.single()()
        settle()

        assertEquals(1, changes.get())
        assertEquals(1, events.filterIsInstance<BackplaneReconnected>().size)
    }

    @Test
    fun `a failed send is reported, and local subscribers still hear the change`() {
        val transport = object : Transport {
            override fun send(topic: Topic) = error("down")
            override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {}
            override fun close() {}
        }
        val bus = node(transport).apply { start() }
        val changes = bus.changes()

        bus.publish(topic)
        settle()

        assertEquals(1, changes.get())
        assertEquals(listOf(topic), events.filterIsInstance<BackplaneSendFailed>().map { it.topic })
    }

    @Test
    fun `publishing on a backplane that was never started fails, naming the fix`() {
        val failure = assertThrows<IllegalStateException> { node().publish(topic) }

        assertEquals("Backplane isn't started: add the bus to the App's Node", failure.message)
    }

    @Test
    fun `every subscriber re-reads once per heal interval, with no publish, spread across it`() {
        val node = node().apply { start() }
        val counts = (1..60).map { node.changes() }

        scheduler.tick(Duration.ofSeconds(30))
        scheduler.runUntilIdle()
        val early = counts.count { it.get() > 0 }

        scheduler.tick(Duration.ofSeconds(270))
        scheduler.runUntilIdle()

        assertEquals(true, early in 1..59)
        assertEquals(setOf(1), counts.map { it.get() }.toSet())
    }

    @Test
    fun `a node hears only the tenants it has watchers for`() {
        val heard = CopyOnWriteArrayList<Topic>()
        val inner = broker.transport()
        val counting = object : Transport by inner {
            override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) =
                inner.start({ heard += it; received(it) }, reconnected)
        }
        val sender = node().apply { start() }
        val listener = node(counting).apply { start() }
        val acme = TenantId.of("acme")(topic)
        val beta = TenantId.of("beta")(topic)
        val watching = listener.subscribe(setOf(acme)) {}

        sender.publish(acme)
        sender.publish(beta)
        watching.close()
        sender.publish(acme)

        assertEquals(listOf(acme), heard)
    }

    @Test
    fun `closing a subscription twice leaves the node hearing the others, for watchers staying and arriving`() {
        val sender = node().apply { start() }
        val listener = node().apply { start() }
        val acme = TenantId.of("acme")(topic)
        val leaving = listener.subscribe(setOf(acme)) {}
        val staying = AtomicInteger()
        listener.subscribe(setOf(acme)) { staying.incrementAndGet() }

        leaving.close()
        leaving.close()
        sender.publish(acme)
        settle()
        assertEquals(1, staying.get())

        val arriving = AtomicInteger()
        listener.subscribe(setOf(acme)) { arriving.incrementAndGet() }
        sender.publish(acme)
        settle()
        assertEquals(1, arriving.get())
    }
}
