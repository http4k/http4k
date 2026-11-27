package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.tenancy.Scope

/**
 * A named, keyed read, null when absent. Reading it records its topics, which is what a region redraws on.
 */
sealed class State<Key : Any, Model> {
    abstract val name: StateName

    internal abstract fun read(tracker: Tracker, key: Key): Model

    /**
     * Reads this state under the key from a derived state or rule, which takes on its topics.
     */
    fun read(ctx: StateContext<*>, key: Key): Model = read(ctx.tracker, key)

    /**
     * Reads this state under the key in a read transaction, for background work and webhooks. Its topics are dropped,
     * so it isn't watched.
     */
    fun read(scope: Scope, key: Key): Model = scope.reading { ctx -> read(ctx, key) }.value
}

/**
 * Reads this state, which has no key, from a derived state or rule.
 */
fun <Model> State<Unit, Model>.read(ctx: StateContext<*>): Model = read(ctx, Unit)

/**
 * Reads this state, which has no key, in a read transaction.
 */
fun <Model> State<Unit, Model>.read(scope: Scope): Model = read(scope, Unit)

internal fun topic(name: StateName, shown: String) = Topic.of(if (shown.isEmpty()) name.value else "$name:$shown")
