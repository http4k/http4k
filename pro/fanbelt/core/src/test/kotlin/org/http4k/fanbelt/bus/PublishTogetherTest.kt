/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

class PublishTogetherTest {
    private val one = Topic.of("one")
    private val two = Topic.of("two")

    private class Runs(private val scheduler: DeterministicScheduler = DeterministicScheduler(Instant.EPOCH)) : ScheduledExecutorService by scheduler {
        override fun execute(command: Runnable) = command.run()
    }

    private class Recording : Transport {
        val sent = mutableListOf<Topic>()
        override fun send(topic: Topic) {
            sent += topic
        }

        override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {}
        override fun close() {}
    }

    private object Two : Feature<Unit, Unit>({}) {
        val both by operation { _, _, _: Unit -> Changes.of(Topic.of("one"), Topic.of("two")) }
    }

    @Test
    fun `other nodes still hear about every topic`() {
        val transport = Recording()
        val bus = Backplane(transport, Runs(), {}, Duration.ofMinutes(5)).apply { start() }

        bus.publish(setOf(one, two))

        assertEquals(listOf(one, two), transport.sent)
    }

    @Test
    fun `an operation publishes what it changed together`() {
        val infra = AppInfra(AppRuntime(scheduler = Runs()), InMemoryDocumentStorage()).apply { start() }
        val scope = infra.scope(TenantId.of("acme"))
        val heard = AtomicInteger()
        scope.bus.subscribe(setOf(one, two)) { heard.incrementAndGet() }

        scope.execute(Two.both, Unit, Unit)

        assertEquals(1, heard.get())
    }
}
