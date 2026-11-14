/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.store.Tables

/**
 * A state that reads a feature's domain. Its topic for a key is what an operation publishes in `Changes` when it
 * changes that value, so which states changed and what redraws are the same thing.
 */
class StoredState<Domain, Key : Any, Model> internal constructor(
    override val name: StateName,
    private val domain: (Tables) -> Domain,
    private val show: (Key) -> String,
    private val reader: (Domain, Key) -> Model,
) : State<Key, Model>() {
    fun topic(key: Key): Topic = topic(name, show(key))

    override fun read(tracker: Tracker, key: Key): Model {
        tracker.watch(topic(key))
        return reader(domain(tracker.tables), key)
    }
}

/**
 * The topic of a stored state with no key.
 */
fun <Domain, Model> StoredState<Domain, Unit, Model>.topic() = topic(Unit)
