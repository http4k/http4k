/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.events.Events
import org.http4k.fanbelt.bus.memory.InMemoryBus
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A Bus shared across nodes, carrying topics over a Transport.
 */
class Backplane(
    private val transport: Transport,
    private val scheduler: ScheduledExecutorService,
    private val events: Events,
    private val heal: Duration,
) : Bus {
    private val local = InMemoryBus(scheduler, events)
    private val ticks = AtomicInteger()
    private val interests = mutableMapOf<Interest, Int>()

    @Volatile
    private var started = false

    @Volatile
    private var healing: ScheduledFuture<*>? = null

    override fun start() {
        transport.start(::received, ::reconnected)
        synchronized(interests) {
            started = true
            interests.keys.forEach(transport::interested)
        }
        val step = heal.toMillis() / SLICES
        healing = scheduler.scheduleWithFixedDelay({ local.publishSlice(ticks.getAndIncrement() % SLICES, SLICES) }, step, step, MILLISECONDS)
    }

    override fun close() {
        healing?.cancel(false)
        transport.close()
    }

    override fun subscribe(topics: Set<Topic>, onChange: () -> Unit) =
        counted(topics.map(Interest::of).toSet()) { local.subscribe(topics, onChange) }

    /**
     * Hears every topic published under an interest, in any tenant, naming each one, and is told when some may have been
     * missed.
     */
    internal fun listen(interest: Interest, heard: (Topic) -> Unit, missed: () -> Unit) =
        counted(setOf(interest)) { local.listen(interest, heard, missed) }

    private fun counted(wanted: Set<Interest>, open: () -> Subscription): Subscription {
        wanted.forEach { interest(it, 1) }
        val subscription = open()
        val closed = AtomicBoolean(false)
        return Subscription {
            if (!closed.getAndSet(true)) {
                subscription.close()
                wanted.forEach { interest(it, -1) }
            }
        }
    }

    private fun interest(interest: Interest, change: Int) = synchronized(interests) {
        val after = interests.merge(interest, change) { count, added -> (count + added).takeIf { it > 0 } }
        if (started && after == 1 && change == 1) transport.interested(interest)
        if (started && after == null) transport.uninterested(interest)
    }

    override fun publish(topics: Set<Topic>) {
        check(started) { "Backplane isn't started: add the bus to the App's Node" }
        local.publish(topics)
        topics.forEach { topic ->
            runCatching { transport.send(topic) }
                .onFailure { events(BackplaneSendFailed(topic, it)) }
        }
    }

    private fun received(topic: Topic) = local.publish(topic)

    private fun reconnected() {
        events(BackplaneReconnected)
        local.publishAll()
    }
}

private const val SLICES = 30
