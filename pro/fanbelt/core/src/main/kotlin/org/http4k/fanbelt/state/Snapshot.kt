/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.tenancy.Scope
import java.time.Instant
import kotlin.reflect.KClass

class Snapshot<R> internal constructor(val value: R, val topics: Set<Topic>, internal val presence: Map<Topic, KClass<out Any>>, val staleAt: Instant?)

internal fun <R> Scope.snapshot(prefetch: Map<Topic, KClass<out Any>>, block: (StateContext<Unit>) -> R): Snapshot<R> =
    snapshotKnowing(if (prefetch.isEmpty()) emptyMap() else presence.present(prefetch), block)

private tailrec fun <R> Scope.snapshotKnowing(
    known: Map<Topic, Map<PresenceMember, Any?>>,
    block: (StateContext<Unit>) -> R
): Snapshot<R> {
    val (result, tracker) = tables { tables ->
        val tracker = Tracker(tables, known)
        block(StateContext(tracker, Unit)) to tracker
    }
    val missing = tracker.presence - known.keys
    if (missing.isEmpty()) return Snapshot(result, tracker.topics, tracker.presence, tracker.staleAt)
    return snapshotKnowing(known + presence.present(missing), block)
}
