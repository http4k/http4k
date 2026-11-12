/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.random
import org.http4k.events.Events
import org.http4k.fanbelt.bus.memory.InMemoryBus
import java.time.Duration
import java.util.Random
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class Backplane(
    private val transport: Transport,
    private val scheduler: ScheduledExecutorService,
    random: Random,
    private val events: Events,
    private val heal: Duration,
) : Bus {
    private val local = InMemoryBus(scheduler, events)
    private val nodeId = NodeId.random(random)
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

    override fun subscribe(topics: Set<Topic>, onChange: () -> Unit): Subscription {
        val wanted = topics.map(Interest::of).toSet()
        wanted.forEach { interest(it, 1) }
        val subscription = local.subscribe(topics, onChange)
        val open = AtomicBoolean(true)
        return Subscription {
            if (open.getAndSet(false)) {
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
            runCatching { transport.send(BackplaneMessage.of(nodeId, topic)) }
                .onFailure { events(BackplaneSendFailed(topic, it)) }
        }
    }

    private fun received(message: BackplaneMessage) {
        if (message.from != nodeId) local.publish(message.topic)
    }

    private fun reconnected() {
        events(BackplaneReconnected(nodeId.toString()))
        local.publishAll()
    }
}

private const val SLICES = 30
