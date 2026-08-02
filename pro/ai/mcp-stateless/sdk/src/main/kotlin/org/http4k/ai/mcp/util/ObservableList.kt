/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.util

import org.http4k.ai.mcp.server.protocol.ObservableCapability
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates.observable

open class ObservableList<T>(initial: Iterable<T>) : ObservableCapability<T>, Iterable<T> {
    private val observers = ConcurrentHashMap<Any, () -> Unit>()

    override var items: Iterable<T> by observable(initial) { _, _, _ -> observers.values.forEach { it() } }

    override fun onChange(key: Any, handler: () -> Unit) {
        observers[key] = handler
    }

    override fun removeObserver(key: Any) {
        observers.remove(key)
    }

    override fun iterator() = items.iterator()
}
