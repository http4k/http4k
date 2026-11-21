/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.at
import org.http4k.fanbelt.state.Snapshot
import org.http4k.fanbelt.tenancy.Scope
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class Trace<Model>(
    private val scope: Scope,
    private val snapshot: (Set<Topic>) -> Snapshot<Model>,
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
    private var presence: Set<Topic> = emptySet()

    private val swapping = ReentrantLock()

    fun read(): Model {
        val current = try {
            val first = remembering()
            if (first.topics == topics) first else follow(first.topics)
        } catch (e: Exception) {
            staleAt(scope.runtime.clock.instant() + RETRY)
            throw e
        }
        staleAt(current.staleAt)
        return current.value
    }

    fun close() = swapping.withLock {
        closed = true
        subscription?.close()
        stale?.cancel(false)
    }

    private fun follow(next: Set<Topic>): Snapshot<Model> {
        swapping.withLock {
            val previous = subscription
            subscription = scope.bus.subscribe(next) { changed() }
            topics = next
            previous?.close()
            if (closed) subscription?.close()
        }
        return remembering()
    }

    private fun remembering() = snapshot(presence).also { presence = it.presence }

    private fun staleAt(at: Instant?) = swapping.withLock {
        stale?.cancel(false)
        stale = if (closed) null else at?.let { scope.runtime.at(it, changed) }
    }
}

private val RETRY = Duration.ofSeconds(5)
