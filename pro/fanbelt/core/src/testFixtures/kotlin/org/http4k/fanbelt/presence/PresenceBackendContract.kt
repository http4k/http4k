/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import org.http4k.fanbelt.bus.Topic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.util.UUID

abstract class PresenceBackendContract {
    protected val clock = SettableClock()

    abstract fun backend(): PresenceBackend

    private val backend by lazy { backend().apply { start() } }
    private val ttl = Duration.ofSeconds(30)
    private val room = Topic.of("room-${UUID.randomUUID()}")
    private val hall = Topic.of("hall-${UUID.randomUUID()}")

    private fun connection(topic: Topic, who: String) = PresenceConnection(topic, ConnectionId.of(UUID.randomUUID()), PresenceMember.of(who))

    private fun PresenceBackend.join(connection: PresenceConnection) = join(listOf(connection), ttl)

    private fun valuesIn(topic: Topic) = backend.present(topic).mapKeys { (who, _) -> who.value }

    @AfterEach
    fun close() = backend.close()

    @Test
    fun `several topics can be read at once`() {
        backend.join(connection(room, "alex"))
        backend.join(connection(hall, "sam"))
        backend.set(hall, PresenceMember.of("sam"), "door")

        val present = backend.present(setOf(room, hall))

        assertThat(present, equalTo(mapOf(room to mapOf(PresenceMember.of("alex") to null), hall to mapOf(PresenceMember.of("sam") to "door"))))
    }

    @Test
    fun `joining again renews every connection's lease, in one call`() {
        val alex = connection(room, "alex")
        val sam = connection(room, "sam")
        val kim = connection(hall, "kim")
        listOf(alex, sam, kim).forEach { backend.join(it) }

        clock.now += Duration.ofSeconds(20)
        backend.join(listOf(alex, sam, kim), ttl)
        clock.now += Duration.ofSeconds(20)

        assertThat(backend.present(room).keys.map { it.value }.toSet(), equalTo(setOf("alex", "sam")))
        assertThat(backend.present(hall).keys.map { it.value }.toSet(), equalTo(setOf("kim")))
    }

    @Test
    fun `a sweep removes expired connections, naming only the topics that lost someone`() {
        val alex = connection(room, "alex")
        val kim = connection(hall, "kim")
        listOf(alex, kim).forEach { backend.join(it) }

        clock.now += Duration.ofSeconds(20)
        backend.join(listOf(kim), ttl)
        clock.now += Duration.ofSeconds(20)

        assertThat(backend.sweep(), hasElement(room))
        assertThat(backend.sweep(), !hasElement(hall))
        assertThat(backend.present(room), equalTo(emptyMap<PresenceMember, String?>()))
        assertThat(backend.present(hall).keys.map { it.value }.toSet(), equalTo(setOf("kim")))
    }

    @Test
    fun `leaving removes the connection straight away`() {
        val alex = connection(room, "alex")
        backend.join(alex)

        backend.leave(alex)

        assertThat(backend.present(room), equalTo(emptyMap<PresenceMember, String?>()))
    }

    @Test
    fun `a member carries one value, whichever of their connections is open`() {
        val alexHere = connection(room, "alex")
        val alexAgain = connection(room, "alex")
        val sam = connection(room, "sam")
        listOf(alexHere, sam).forEach { backend.join(it) }

        backend.set(room, alexHere.member, "B5")
        backend.join(alexAgain)
        backend.leave(alexHere)

        assertThat(valuesIn(room), equalTo(mapOf("alex" to "B5", "sam" to null)))
    }

    @Test
    fun `a member's value goes when they leave, and doesn't come back when they return`() {
        val alex = connection(room, "alex")
        backend.join(alex)
        backend.set(room, alex.member, "B5")

        backend.leave(alex)
        backend.join(connection(room, "alex"))

        assertThat(valuesIn(room), equalTo(mapOf("alex" to null)))
    }

    @Test
    fun `a member's value goes when their lease expires`() {
        val alex = connection(room, "alex")
        backend.join(alex)
        backend.set(room, alex.member, "B5")

        clock.now += Duration.ofSeconds(40)
        backend.sweep()
        backend.join(connection(room, "alex"))

        assertThat(valuesIn(room), equalTo(mapOf("alex" to null)))
    }

    @Test
    fun `a value for someone who isn't here is ignored`() {
        backend.set(room, PresenceMember.of("ghost"), "B5")
        backend.join(connection(room, "ghost"))

        assertThat(valuesIn(room), equalTo(mapOf("ghost" to null)))
    }
}

class SettableClock : Clock() {
    var now: Instant = Instant.parse("2026-01-01T09:00:00Z")
    override fun getZone(): ZoneId = UTC
    override fun withZone(zone: ZoneId) = this
    override fun instant() = now
}
