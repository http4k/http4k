/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.events.Events
import org.http4k.fanbelt.bus.memory.InMemoryBus
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A Bus shared across nodes, carrying topics over a Transport.
 */
internal class Backplane(
    private val transport: Transport,
    private val scheduler: ScheduledExecutorService,
    private val events: Events,
    private val heal: Duration,
) : Bus {
    private val local = InMemoryBus(scheduler, events)
    private val ticks = AtomicInteger()
    private val slots = ConcurrentHashMap<Interest, Slot>()

    @Volatile
    private var started = false

    @Volatile
    private var healing: ScheduledFuture<*>? = null

    override fun start() {
        transport.start(::received, ::reconnected)
        started = true
        slots.forEach { (interest, slot) -> synchronized(slot) { slot.reconcile(interest) } }
        val step = heal.toMillis() / SLICES
        healing = scheduler.scheduleWithFixedDelay({ heal(ticks.getAndIncrement() % SLICES) }, step, step, MILLISECONDS)
    }

    override fun close() {
        healing?.cancel(false)
        transport.close()
    }

    override fun subscribe(topics: Set<Topic>, onChange: () -> Unit) =
        counted(topics.map(Interest::of).toSet()) { local.subscribe(topics, onChange) }

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

    private fun interest(interest: Interest, change: Int) {
        val slot = slots.computeIfAbsent(interest) { Slot() }
        synchronized(slot) {
            slot.count += change
            if (started) slot.reconcile(interest)
        }
    }

    private fun Slot.reconcile(interest: Interest) {
        when {
            count > 0 && !subscribed -> {
                transport.interested(interest)
                subscribed = true
            }

            count <= 0 && subscribed -> {
                subscribed = false
                transport.uninterested(interest)
            }
        }
    }

    override fun publish(topics: Set<Topic>) {
        check(started) { "Backplane isn't started: add the bus to the App's Node" }
        if (topics.isEmpty()) return
        local.publish(topics)
        runCatching { transport.send(topics) }
            .onFailure { events(BackplaneSendFailed(topics, it)) }
    }

    private fun heal(slice: Int) {
        local.publishSlice(slice, SLICES)
        if (slice == SLICES - 1) local.missed()
    }

    private fun received(topic: Topic) = local.publish(topic)

    private fun reconnected() {
        events(BackplaneReconnected)
        local.publishSlice(0, 1)
    }
}

private const val SLICES = 30

private class Slot {
    var count = 0
    var subscribed = false
}
