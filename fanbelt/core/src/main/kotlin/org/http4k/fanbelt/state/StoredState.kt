package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.store.Tables

/**
 * A state read from a feature's domain.
 */
class StoredState<Domain, Key : Any, Model> internal constructor(
    override val name: StateName,
    private val domain: (Tables) -> Domain,
    internal val show: (Key) -> String,
    private val reader: (Domain, Key) -> Model,
) : State<Key, Model>() {
    fun topic(key: Key): Topic = topic(name, show(key))

    override fun read(tracker: Tracker, key: Key): Model {
        tracker.watch(topic(key))
        return reader(domain(tracker.tables), key)
    }
}

fun <Domain, Model> StoredState<Domain, Unit, Model>.topic() = topic(Unit)
