/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.tenancy.Scope
import java.time.Instant
import kotlin.reflect.KClass

class StateContext<Domain> internal constructor(internal val tracker: Tracker, val domain: Domain) {
    fun watch(vararg topics: Topic) = tracker.watch(*topics)

    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model = state.read(tracker, key)

    fun <Model> read(state: State<Unit, Model>): Model = read(state, Unit)

    fun staleAt(at: Instant) = tracker.staleAt(at)
}

internal class Tracker(
    internal val tables: Tables,
    private val known: Map<Topic, Map<PresenceMember, Any?>>?
) {
    private val watched = mutableSetOf<Topic>()
    private val presenceRead = mutableMapOf<Topic, KClass<out Any>>()

    internal val topics: Set<Topic> get() = watched.toSet()

    internal val presence: Map<Topic, KClass<out Any>> get() = presenceRead.toMap()

    internal var staleAt: Instant? = null
        private set

    fun watch(vararg topics: Topic) {
        watched += topics
    }

    fun staleAt(at: Instant) {
        staleAt = staleAt?.let { minOf(it, at) } ?: at
    }

    fun present(topic: Topic, type: KClass<out Any>): Map<PresenceMember, Any?> {
        checkNotNull(known) { "$topic is presence, which an operation can't see" }
        presenceRead[topic] = type
        return known[topic].orEmpty()
    }
}

fun <R> Scope.reading(block: (StateContext<Unit>) -> R): Snapshot<R> = snapshot(emptyMap(), block)
