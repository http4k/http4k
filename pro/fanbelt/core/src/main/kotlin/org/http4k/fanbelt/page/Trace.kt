/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.state.Snapshot
import org.http4k.fanbelt.tenancy.Scope
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.reflect.KClass

internal class Trace<Model>(
    private val scope: Scope,
    private val snapshot: (Map<Topic, KClass<out Any>>) -> Snapshot<Model>,
    private val changed: () -> Unit,
) {
    @Volatile
    private var closed = false

    @Volatile
    private var topics: Set<Topic> = emptySet()

    @Volatile
    private var subscription: Subscription? = null

    @Volatile
    private var stale: ScheduledFuture<*>? = null

    @Volatile
    private var presence: Map<Topic, KClass<out Any>> = emptyMap()

    fun read(): Model {
        val first = remembering()
        val current = if (first.topics == topics) first else follow(first.topics)
        staleAt(current.staleAt)
        return current.value
    }

    fun close() {
        closed = true
        subscription?.close()
        stale?.cancel(false)
    }

    private fun follow(next: Set<Topic>): Snapshot<Model> {
        val previous = subscription
        topics = next
        subscription = scope.bus.subscribe(next) { changed() }
        previous?.close()
        if (closed) subscription?.close()
        return remembering()
    }

    private fun remembering() = snapshot(presence).also { presence = it.presence }

    private fun staleAt(at: Instant?) {
        stale?.cancel(false)
        stale = if (at == null) {
            null
        } else {
            scope.runtime.scheduler.schedule(changed, Duration.between(scope.runtime.clock.instant(), at).toNanos().coerceAtLeast(0), NANOSECONDS)
        }
    }
}
