/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import org.http4k.fanbelt.bus.Topic

sealed class State<Key : Any, Model> {
    abstract val name: StateName

    internal abstract fun read(tracker: Tracker, key: Key): Model
}

internal fun topic(name: StateName, shown: String) = Topic.of(if (shown.isEmpty()) name.value else "$name:$shown")
