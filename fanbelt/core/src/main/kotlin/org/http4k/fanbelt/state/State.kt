package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic

/**
 * A named, keyed read, null when absent. Reading it records its topics, which is what a region redraws on.
 */
sealed class State<Key : Any, Model> {
    abstract val name: StateName

    internal abstract fun read(tracker: Tracker, key: Key): Model
}

internal fun topic(name: StateName, shown: String) = Topic.of(if (shown.isEmpty()) name.value else "$name:$shown")
