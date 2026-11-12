/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.fanbelt.bus.memory.InMemoryBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class InMemoryBusTest {
    private object Inline : AbstractExecutorService() {
        override fun execute(command: Runnable) = command.run()
        override fun shutdown() {}
        override fun shutdownNow() = emptyList<Runnable>()
        override fun isShutdown() = false
        override fun isTerminated() = false
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
    }

    @Test
    fun `subscribers hear of a change in the order they subscribed, so a run can be replayed`() {
        val bus = InMemoryBus(Inline) {}
        val heard = mutableListOf<Int>()
        (1..50).forEach { n -> bus.subscribe(setOf(Topic.of("t"))) { heard += n } }

        bus.publish(Topic.of("t"))

        assertEquals((1..50).toList(), heard)
    }

    @Test
    fun `a subscriber joining as the topic's last other subscriber leaves still hears the next change`() {
        val bus = InMemoryBus(Inline) {}
        val topic = setOf(Topic.of("t"))

        val rounds = 200_000
        val started = AtomicInteger()
        val left = AtomicInteger()
        val leaving = AtomicReference<Subscription>()
        val leaver = Thread.ofPlatform().start {
            (1..rounds).forEach { round ->
                while (started.get() < round) Thread.onSpinWait()
                leaving.get().close()
                left.set(round)
            }
        }

        val deaf = (1..rounds).count { round ->
            leaving.set(bus.subscribe(topic) {})
            val heard = AtomicBoolean(false)
            started.set(round)
            val joined = bus.subscribe(topic) { heard.set(true) }
            while (left.get() < round) Thread.onSpinWait()

            bus.publish(topic)
            joined.close()
            !heard.get()
        }
        leaver.join()

        assertEquals(0, deaf)
    }
}
