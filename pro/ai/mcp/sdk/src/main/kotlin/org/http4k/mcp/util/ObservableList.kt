package org.http4k.mcp.util

import kotlin.properties.Delegates

abstract class ObservableList<T>(initial: Iterable<T>) : Observable(), Iterable<T> {

    var items by Delegates.observable(initial) { _, _, _ -> callbacks.values.forEach { it() } }

    override fun iterator() = items.iterator()
}
