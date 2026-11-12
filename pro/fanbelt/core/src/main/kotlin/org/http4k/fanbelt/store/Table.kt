/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import org.http4k.fanbelt.store.Order.Oldest

interface Table<ID : Any, T : Identified<ID>> {
    fun page(where: Where<T> = everything(), order: Order<T> = Oldest, after: Cursor? = null, limit: Int? = null): Rows<T>
    fun count(where: Where<T> = everything()): Int
    fun get(id: ID): T?
    fun insert(row: T)
    fun update(id: ID, change: (T) -> T)
    fun delete(id: ID)
}

fun <ID : Any, T : Identified<ID>> Table<ID, T>.first(where: Where<T> = everything(), order: Order<T> = Oldest): T? =
    page(where, order, limit = 1).firstOrNull()
