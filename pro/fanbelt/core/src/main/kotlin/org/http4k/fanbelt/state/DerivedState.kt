/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import org.http4k.fanbelt.store.Tables

internal class DerivedState<Domain, Key : Any, Model> internal constructor(
    override val name: StateName,
    private val domain: (Tables) -> Domain,
    private val reader: (StateContext<Domain>, Key) -> Model,
) : State<Key, Model>() {
    override fun read(tracker: Tracker, key: Key): Model = reader(StateContext(tracker, domain(tracker.tables)), key)

    companion object {
        operator fun <Key : Any, Model> invoke(name: String, read: (StateContext<Unit>, Key) -> Model) =
            DerivedState(StateName.of(name), { }, read)
    }
}
