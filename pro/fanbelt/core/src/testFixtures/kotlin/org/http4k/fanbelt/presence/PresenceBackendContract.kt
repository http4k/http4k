/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import org.http4k.fanbelt.bus.Topic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun valuesIn(topic: Topic) = backend.present(topic, Spot::class).entries.associate { (who, spot) -> who.value to spot?.cell }

    @AfterEach
    fun close() = backend.close()

    @Test
    fun `several topics can be read at once`() {
        backend.join(connection(room, "alex"))
        backend.join(connection(hall, "sam"))
        backend.set(hall, PresenceMember.of("sam"), Spot("door"))

        val present = backend.present(mapOf(room to Spot::class, hall to Spot::class))

        assertEquals(mapOf(room to mapOf(PresenceMember.of("alex") to null), hall to mapOf(PresenceMember.of("sam") to Spot("door"))), present)
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

        assertEquals(setOf("alex", "sam"), backend.present(room, Unit::class).keys.map { it.value }.toSet())
        assertEquals(setOf("kim"), backend.present(hall, Unit::class).keys.map { it.value }.toSet())
    }

    @Test
    fun `a sweep removes expired connections, naming only the topics that lost someone`() {
        val alex = connection(room, "alex")
        val kim = connection(hall, "kim")
        listOf(alex, kim).forEach { backend.join(it) }

        clock.now += Duration.ofSeconds(20)
        backend.join(listOf(kim), ttl)
        clock.now += Duration.ofSeconds(20)

        assertEquals(true, room in backend.sweep())
        assertEquals(false, hall in backend.sweep())
        assertEquals(emptyMap<PresenceMember, Unit?>(), backend.present(room, Unit::class))
        assertEquals(setOf("kim"), backend.present(hall, Unit::class).keys.map { it.value }.toSet())
    }

    @Test
    fun `leaving removes the connection straight away`() {
        val alex = connection(room, "alex")
        backend.join(alex)

        backend.leave(alex)

        assertEquals(emptyMap<PresenceMember, Unit?>(), backend.present(room, Unit::class))
    }

    @Test
    fun `a member carries one value, whichever of their connections is open`() {
        val alexHere = connection(room, "alex")
        val alexAgain = connection(room, "alex")
        val sam = connection(room, "sam")
        listOf(alexHere, sam).forEach { backend.join(it) }

        backend.set(room, alexHere.member, Spot("B5"))
        backend.join(alexAgain)
        backend.leave(alexHere)

        assertEquals(mapOf("alex" to "B5", "sam" to null), valuesIn(room))
    }

    @Test
    fun `a member's value goes when they leave, and doesn't come back when they return`() {
        val alex = connection(room, "alex")
        backend.join(alex)
        backend.set(room, alex.member, Spot("B5"))

        backend.leave(alex)
        backend.join(connection(room, "alex"))

        assertEquals(mapOf("alex" to null), valuesIn(room))
    }

    @Test
    fun `a member's value goes when their lease expires`() {
        val alex = connection(room, "alex")
        backend.join(alex)
        backend.set(room, alex.member, Spot("B5"))

        clock.now += Duration.ofSeconds(40)
        backend.sweep()
        backend.join(connection(room, "alex"))

        assertEquals(mapOf("alex" to null), valuesIn(room))
    }

    @Test
    fun `a value for someone who isn't here is ignored`() {
        backend.set(room, PresenceMember.of("ghost"), Spot("B5"))
        backend.join(connection(room, "ghost"))

        assertEquals(mapOf("ghost" to null), valuesIn(room))
    }
}

data class Spot(val cell: String)

class SettableClock : Clock() {
    var now: Instant = Instant.parse("2026-01-01T09:00:00Z")
    override fun getZone(): ZoneId = UTC
    override fun withZone(zone: ZoneId) = this
    override fun instant() = now
}
