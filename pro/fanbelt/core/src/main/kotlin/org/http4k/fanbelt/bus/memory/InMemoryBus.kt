/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.memory

import org.http4k.events.Events
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.Subscriber
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.Topic
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService

internal class InMemoryBus(private val executor: ExecutorService, private val events: Events) : Bus {
    private val subscribers = ConcurrentHashMap<Topic, CopyOnWriteArraySet<Subscriber>>()
    private val listeners = ConcurrentHashMap<Interest, CopyOnWriteArraySet<Listener>>()

    override fun publish(topics: Set<Topic>) {
        topics.flatMap { subscribers[it].orEmpty() }.toSet().forEach { it.markDirty() }
        topics.forEach { topic -> listeners[Interest.of(topic)].orEmpty().forEach { it.heard(topic) } }
    }

    fun publishAll() {
        publishSlice(0, 1)
        listeners.values.flatten().toSet().forEach { it.missed() }
    }

    fun publishSlice(slice: Int, slices: Int) =
        subscribers.values.asSequence().flatten().filter { Math.floorMod(System.identityHashCode(it), slices) == slice }.toSet().forEach { it.markDirty() }

    override fun subscribe(topics: Set<Topic>, onChange: () -> Unit) = subscribers.join(topics, Subscriber(executor, events, onChange))

    fun listen(interest: Interest, heard: (Topic) -> Unit, missed: () -> Unit) = listeners.join(setOf(interest), Listener(heard, missed))

    private class Listener(val heard: (Topic) -> Unit, val missed: () -> Unit)
}

private fun <K : Any, V : Any> ConcurrentHashMap<K, CopyOnWriteArraySet<V>>.join(keys: Set<K>, value: V): Subscription {
    keys.forEach { compute(it) { _, set -> (set ?: CopyOnWriteArraySet()).apply { add(value) } } }
    return Subscription { keys.forEach { key -> computeIfPresent(key) { _, set -> set.apply { remove(value) }.ifEmpty { null } } } }
}
