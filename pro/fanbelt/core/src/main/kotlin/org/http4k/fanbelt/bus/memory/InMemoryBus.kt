/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.memory

import org.http4k.events.Events
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.Subscriber
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService

internal class InMemoryBus(private val executor: ExecutorService, private val events: Events) : Bus {
    private val subscribers = ConcurrentHashMap<Topic, CopyOnWriteArraySet<Subscriber>>()

    override fun publish(topics: Set<Topic>) = topics.flatMap { subscribers[it].orEmpty() }.toSet().forEach { it.markDirty() }

    fun publishAll() = publishSlice(0, 1)

    fun publishSlice(slice: Int, slices: Int) =
        subscribers.values.asSequence().flatten().filter { Math.floorMod(System.identityHashCode(it), slices) == slice }.toSet().forEach { it.markDirty() }

    override fun subscribe(topics: Set<Topic>, onChange: () -> Unit): Subscription {
        val subscriber = Subscriber(executor, events, onChange)
        topics.forEach { subscribers.compute(it) { _, set -> (set ?: CopyOnWriteArraySet()).apply { add(subscriber) } } }
        return Subscription { topics.forEach { topic -> subscribers.computeIfPresent(topic) { _, set -> set.apply { remove(subscriber) }.ifEmpty { null } } } }
    }
}
