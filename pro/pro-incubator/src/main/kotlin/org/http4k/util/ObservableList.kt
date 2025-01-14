package org.http4k.util

import kotlin.properties.Delegates

abstract class ObservableList<T>(initial: List<T>) : Iterable<T> {
    private val handlers = mutableListOf<() -> Any>()

    var items by Delegates.observable(initial) { _, _, _ -> handlers.forEach { it() } }

    fun onChange(handler: () -> Any) = handlers.add(handler)

    override fun iterator() = items.iterator()
}
