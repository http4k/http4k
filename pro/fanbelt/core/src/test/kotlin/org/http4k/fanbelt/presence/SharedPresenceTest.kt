/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.events.Event
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.memory.InMemoryBus
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
        override fun set(topic: Topic, member: PresenceMember, value: String) {}
        override fun present(topic: Topic) = emptyMap<PresenceMember, String?>()
        override fun sweep(): Set<Topic> = emptySet<Topic>().also { sweeps.incrementAndGet() }
    }

    private val heartbeat = Duration.ofSeconds(10)

    private val bus = InMemoryBus(scheduler) {}

    private fun started() = SharedPresence(backend, bus, scheduler, Random(0), { events += it }, ttl = Duration.ofSeconds(30), heartbeat = heartbeat).apply { start() }

    private fun beats(count: Int) = repeat(count) { scheduler.tick(heartbeat) }

    @Test
    fun `every heartbeat renews all of a node's connections in one call`() {
        val presence = started()
        presence.join(good, PresenceMember.of("alex"))
        presence.join(good, PresenceMember.of("sam"))
        presence.join(other, PresenceMember.of("alex"))

        beats(1)

        assertThat(renewals.map { it.size }, equalTo(listOf(1, 1, 1, 3)))
    }

    @Test
    fun `a renewal failing is reported without stopping the sweep, and renewing again is reported once`() {
        val presence = started()
        presence.join(good, PresenceMember.of("alex"))
        failing.set(true)

        beats(3)
        assertThat(sweeps.get(), equalTo(3))
        assertThat(events.filterIsInstance<PresenceRenewalFailed>().size, equalTo(3))

        failing.set(false)
        beats(2)
        assertThat(events.filterIsInstance<PresenceRecovered>().size, equalTo(1))
    }

    @Test
    fun `renewing again republishes every topic the node rejoined, so other nodes see its members return`() {
        val presence = started()
        presence.join(good, PresenceMember.of("alex"))
        presence.join(other, PresenceMember.of("sam"))
        val heard = CopyOnWriteArrayList<Topic>()
        bus.subscribe(setOf(good)) { heard += good }
        bus.subscribe(setOf(other)) { heard += other }
        failing.set(true)
        beats(4)
        assertThat(heard.toSet(), equalTo(emptySet()))

        failing.set(false)
        beats(1)

        assertThat(heard.toSet(), equalTo(setOf(good, other)))
    }

    @Test
    fun `a join the backend refuses is never renewed, so its member doesn't appear once the backend recovers`() {
        val presence = started()
        failing.set(true)
        assertThat({ presence.join(good, PresenceMember.of("alex")) }, throws<IllegalStateException>())

        failing.set(false)
        beats(1)

        assertThat(renewals, equalTo(listOf(emptyList())))
    }

    @Test
    fun `joining presence that was never started fails, naming the fix`() {
        val presence = SharedPresence(backend, InMemoryBus(scheduler) {}, scheduler, Random(0), { events += it }, Duration.ofSeconds(30), Duration.ofSeconds(10))

        assertThat({ presence.join(good, PresenceMember.of("alex")) }, throws<IllegalStateException>(has(Throwable::message, equalTo("SharedPresence isn't started: add it to the App's Node"))))
    }
}
