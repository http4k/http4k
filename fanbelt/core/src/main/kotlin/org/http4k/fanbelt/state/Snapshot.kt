package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.tenancy.Scope
import java.time.Instant

/**
 * A value read from states, with the topics it read.
 */
class Snapshot<R> internal constructor(val value: R, val topics: Set<Topic>, internal val presence: Set<Topic>, val staleAt: Instant?)

internal fun <R> Scope.snapshot(prefetch: Set<Topic>, block: (StateContext<Unit>) -> R): Snapshot<R> =
    snapshotKnowing(if (prefetch.isEmpty()) emptyMap() else presence.present(prefetch), block)

private tailrec fun <R> Scope.snapshotKnowing(
    known: Map<Topic, Map<PresenceMember, String?>>,
    block: (StateContext<Unit>) -> R
): Snapshot<R> {
    val (result, tracker) = tables { tables ->
        val tracker = Tracker(tenant, tables, known, runtime.clock.instant())
        block(StateContext(tracker, Unit)) to tracker
    }
    val missing = tracker.presence - known.keys
    if (missing.isEmpty()) return Snapshot(result, tracker.topics, tracker.presence, tracker.staleAt)
    return snapshotKnowing(known + presence.present(missing), block)
}
