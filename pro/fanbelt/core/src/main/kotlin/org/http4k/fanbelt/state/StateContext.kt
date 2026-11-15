/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Instant

/**
 * What derived states and rules read through: read for other states, watch for topics, staleAt to schedule a re-read,
 * the untracked domain, and the tenant
 */
class StateContext<Domain> internal constructor(internal val tracker: Tracker, val domain: Domain) {
    val tenant: TenantId get() = tracker.tenant

    fun watch(vararg topics: Topic) = tracker.watch(*topics)

    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model = state.read(tracker, key)

    fun <Model> read(state: State<Unit, Model>): Model = read(state, Unit)

    fun staleAt(at: Instant) = tracker.staleAt(at)
}

internal class Tracker(
    internal val tenant: TenantId,
    internal val tables: Tables,
    private val known: Map<Topic, Map<PresenceMember, String?>>?
) {
    private val watched = mutableSetOf<Topic>()
    private val presenceRead = mutableSetOf<Topic>()

    internal val topics: Set<Topic> get() = watched.toSet()

    internal val presence: Set<Topic> get() = presenceRead.toSet()

    internal var staleAt: Instant? = null
        private set

    fun watch(vararg topics: Topic) {
        watched += topics
    }

    fun staleAt(at: Instant) {
        staleAt = staleAt?.let { minOf(it, at) } ?: at
    }

    fun present(topic: Topic): Map<PresenceMember, String?> {
        checkNotNull(known) { "$topic is presence, which an operation can't see" }
        presenceRead += topic
        return known[topic].orEmpty()
    }
}

/**
 * Reads states consistently in one read transaction, returning the value with its topics.
 */
fun <R> Scope.reading(block: (StateContext<Unit>) -> R): Snapshot<R> = snapshot(emptySet(), block)
