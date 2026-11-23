package org.http4k.fanbelt.store

import dev.forkhandles.values.Value
import org.http4k.fanbelt.store.Order.Oldest

/**
 * A document table for one tenant. Every field you filter on needs an index.
 */
interface Table<ID : Value<*>, T : Identified<ID>> {
    fun page(where: Where<T> = everything(), order: Order<T> = Oldest, after: Cursor? = null, limit: Int? = null): Rows<T>
    fun count(where: Where<T> = everything()): Int
    fun get(id: ID): T?
    fun insert(row: T)
    fun update(id: ID, change: (T) -> T)
    fun delete(id: ID)
}

/**
 * The first matching row, or null.
 */
fun <ID : Value<*>, T : Identified<ID>> Table<ID, T>.first(where: Where<T> = everything(), order: Order<T> = Oldest): T? =
    page(where, order, limit = 1).firstOrNull()
