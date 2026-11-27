package org.http4k.fanbelt.state

import java.time.Instant
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Access
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Rule
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId

/**
 * What derived states and rules read through: read for other states, watch for topics, stale to schedule a re-read,
 * the untracked domain, and the tenant
 */
class StateContext<Domain> internal constructor(internal val tracker: Tracker, val domain: Domain) {
    val tenant: TenantId get() = tracker.tenant

    fun watch(vararg topics: Topic) = tracker.watch(*topics)

    /**
     * Reads a state under the key, taking on its topics.
     */
    fun <Key : Any, Model> read(state: State<Key, Model>, key: Key): Model = state.read(tracker, key)

    /**
     * Reads a state with no key, taking on its topics.
     */
    fun <Model> read(state: State<Unit, Model>): Model = read(state, Unit)

    /**
     * Reads a rule for an actor and a key.
     */
    fun <Actor : Any, Key : Any, Found> read(rule: Rule<Actor, Key, Found>, actor: Actor, key: Key): Access<Found> = read(rule, ActorKey(actor, key))

    /**
     * Reads a rule that takes no key, for an actor.
     */
    @JvmName("readForUnitRule")
    fun <Actor : Any, Found> read(rule: Rule<Actor, Unit, Found>, actor: Actor): Access<Found> = read(rule, actor, Unit)

    fun stale(at: Instant) = tracker.stale(at)
}

internal class Tracker(
    internal val tenant: TenantId,
    internal val tables: Tables,
    private val known: Map<Topic, Map<PresenceMember, String?>>?,
    private val now: Instant,
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

    fun stale(at: Instant) {
        if (at > now) staleAt = staleAt?.let { minOf(it, at) } ?: at
    }

    internal fun present(topic: Topic): Map<PresenceMember, String?> {
        checkNotNull(known) { "$topic is presence, which an operation can't see" }
        presenceRead += topic
        return known[topic].orEmpty()
    }
}

/**
 * Reads states consistently in one read transaction, returning the value with its topics.
 */
fun <R> Scope.reading(block: (StateContext<Unit>) -> R): Snapshot<R> = snapshot(emptySet(), block)
