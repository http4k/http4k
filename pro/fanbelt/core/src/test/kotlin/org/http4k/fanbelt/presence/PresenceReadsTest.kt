/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acme
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.reading
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class PresenceReadsTest {
    private class Who private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Who>(::Who)
    }

    private object Room : Feature<Unit, Unit>({}) {
        val here by presence(Who)
        val inRoom by presence(Who, Who)
    }

    private val reading = AtomicBoolean(false)
    private val asked = AtomicInteger()
    private val askedWhileReading = AtomicInteger()
    private val reads = AtomicInteger()

    private val presence = object : PresenceBackend {
        override fun <V : Any> present(topic: Topic, type: KClass<V>): Map<PresenceMember, V?> {
            ask()
            return mapOf(PresenceMember.of("alex") to null)
        }

        override fun present(topics: Map<Topic, KClass<out Any>>): Map<Topic, Map<PresenceMember, Any?>> {
            ask()
            return topics.mapValues { mapOf(PresenceMember.of("alex") to null) }
        }

        private fun ask() {
            asked.incrementAndGet()
            if (reading.get()) askedWhileReading.incrementAndGet()
        }

        override fun join(connections: List<PresenceConnection>, ttl: Duration) {}
        override fun leave(connection: PresenceConnection) {}
        override fun set(topic: Topic, member: PresenceMember, value: Any) {}
        override fun sweep() = emptySet<Topic>()
    }

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val scope = AppInfra(AppRuntime(scheduler = scheduler), presenceBackend = presence).apply { start() }.scope(acme)

    private val viewers = DerivedState<Unit, Set<String>>("viewers") { ctx, _ ->
        reads.incrementAndGet()
        reading.set(true)
        try {
            ctx.read(Room.here).keys.map { it.value }.toSet()
        } finally {
            reading.set(false)
        }
    }

    @Test
    fun `presence is read outside the storage transaction, and the read sees it`() {
        assertEquals(setOf("alex"), scope.reading { ctx -> ctx.read(viewers) }.value)
        assertEquals(0, askedWhileReading.get())
    }

    @Test
    fun `a read of many presence topics asks the backend once`() {
        val seen = scope.reading { ctx -> listOf("a", "b", "c").map { ctx.read(Room.inRoom, Who.of(it)).size } }

        assertEquals(listOf(1, 1, 1), seen.value)
        assertEquals(1, asked.get())
    }

    @Test
    fun `a region remembers which presence it read, so later redraws read once`() {
        val region = Region("viewers", { """<p id="viewers">$it</p>""" }, viewers) { names, _ -> Names(names) }
        region.watch(scope, Request(GET, "/")) {}
        reads.set(0)

        scope.bus.publish(Topic.of("here"))
        scheduler.runUntilIdle()

        assertEquals(1, reads.get())
        assertEquals(0, askedWhileReading.get())
    }

    private data class Names(val names: Set<String>) : ViewModel
}
