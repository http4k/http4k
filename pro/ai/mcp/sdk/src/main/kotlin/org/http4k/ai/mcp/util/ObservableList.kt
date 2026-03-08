/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.util

import kotlin.properties.Delegates.observable

abstract class ObservableList<T>(initial: Iterable<T>) : Observable(), Iterable<T> {

    var items by observable(initial) { _, _, _ -> callbacks.values.forEach { it() } }

    override fun iterator() = items.iterator()
}
