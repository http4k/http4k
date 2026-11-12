/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.events.Event
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.memory.InMemoryBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class SharedPresenceTest {
    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val events = CopyOnWriteArrayList<Event>()
    private val good = Topic.of("good")
    private val other = Topic.of("other")
    private val failing = AtomicBoolean(false)
    private val renewals = CopyOnWriteArrayList<List<PresenceConnection>>()
    private val sweeps = AtomicInteger()

    private val backend = object : PresenceBackend {
        override fun join(connections: List<PresenceConnection>, ttl: Duration) {
            if (failing.get()) error("can't renew")
            renewals += connections
        }

        override fun leave(connection: PresenceConnection) {}
        override fun set(topic: Topic, member: PresenceMember, value: Any) {}
        override fun <V : Any> present(topic: Topic, type: KClass<V>) = emptyMap<PresenceMember, V?>()
        override fun sweep(): Set<Topic> = emptySet<Topic>().also { sweeps.incrementAndGet() }
    }

    private val heartbeat = Duration.ofSeconds(10)

    private fun started() = SharedPresence(backend, InMemoryBus(scheduler) {}, scheduler, Random(0), { events += it }, ttl = Duration.ofSeconds(30), heartbeat = heartbeat).apply { start() }

    private fun beats(count: Int) = repeat(count) { scheduler.tick(heartbeat) }

    @Test
    fun `every heartbeat renews all of a node's connections in one call`() {
        val presence = started()
        presence.join(good, PresenceMember.of("alex"))
        presence.join(good, PresenceMember.of("sam"))
        presence.join(other, PresenceMember.of("alex"))

        beats(1)

        assertEquals(listOf(1, 1, 1, 3), renewals.map { it.size })
    }

    @Test
    fun `a renewal failing is reported without stopping the sweep, and renewing again is reported once`() {
        val presence = started()
        presence.join(good, PresenceMember.of("alex"))
        failing.set(true)

        beats(3)
        assertEquals(3, sweeps.get())
        assertEquals(3, events.filterIsInstance<PresenceRenewalFailed>().size)

        failing.set(false)
        beats(2)
        assertEquals(1, events.filterIsInstance<PresenceRecovered>().size)
    }

    @Test
    fun `joining presence that was never started fails, naming the fix`() {
        val presence = SharedPresence(backend, InMemoryBus(scheduler) {}, scheduler, Random(0), { events += it }, Duration.ofSeconds(30), Duration.ofSeconds(10))

        val failure = assertThrows<IllegalStateException> { presence.join(good, PresenceMember.of("alex")) }

        assertEquals("SharedPresence isn't started: add it to the App's Node", failure.message)
    }
}
