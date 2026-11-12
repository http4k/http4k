/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.memory

import dev.forkhandles.tx.RetryPolicy
import dev.forkhandles.tx.Transactional
import dev.forkhandles.tx.increasingBackoff
import dev.forkhandles.tx.maxAttempts
import dev.forkhandles.tx.mem.InMemoryTransaction
import dev.forkhandles.tx.mem.InMemoryTransactor
import dev.forkhandles.tx.withAdditiveJitter
import dev.forkhandles.values.Value
import org.http4k.fanbelt.store.Among
import org.http4k.fanbelt.store.Cursor
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Order
import org.http4k.fanbelt.store.Rows
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.Where
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import kotlin.reflect.KClass

fun InMemoryDocumentStorage(
    retryPolicy: RetryPolicy = increasingBackoff(Duration.ofMillis(1)).withAdditiveJitter().maxAttempts(50),
): DocumentStorage = InMemoryStorage(InMemoryTransactor(emptyMap(), ::InMemoryTenants, retryPolicy))

private class InMemoryStorage(private val transactor: Transactional<Tenants>) : DocumentStorage {
    override fun <R> write(work: (Tenants) -> R) = transactor.perform { work(OneTenant(it)) }
    override fun <R> read(work: (Tenants) -> R) = transactor.perform(work)
}

private class OneTenant(private val tenants: Tenants) : Tenants by tenants {
    private var owner: TenantId? = null

    override fun tables(tenant: TenantId): Tables {
        check(owner == null || owner == tenant) { "A write belongs to one tenant: it has $owner, so it can't also write $tenant" }
        owner = tenant
        return tenants.tables(tenant)
    }
}

private class Stored(val seq: Long, val row: Identified<*>)

private typealias TenantRows = Map<TenantId, Map<TableName, List<Stored>>>

private fun <T> Where<T>.filterable() = apply {
    clauses.filterIsInstance<Among<T>>().forEach { clause ->
        clause.values.filterNotNull().forEach {
            require(it.serialisesAsJsonString()) { "Can only filter on fields that serialise as JSON strings, but ${clause.field.path.joinToString(".")} is $it" }
        }
    }
}

private fun Any.serialisesAsJsonString(): Boolean = when (this) {
    is Value<*> -> value.serialisesAsJsonString()
    is Number, is Boolean, is Collection<*>, is Map<*, *> -> false
    else -> true
}

private class InMemoryTenants(private val tx: InMemoryTransaction<TenantRows>) : Tenants {
    override fun tables(tenant: TenantId): Tables = InMemoryTables(tx, tenant)

    @Suppress("UNCHECKED_CAST")
    override fun <T> having(name: TableName, where: Where<T>) =
        where.filterable().let { checked -> tx.state.filterValues { tables -> tables[name].orEmpty().any { checked.matches(it.row as T) } }.keys }
}

private class InMemoryTables(
    private val tx: InMemoryTransaction<TenantRows>,
    private val tenant: TenantId,
) : Tables {
    override fun <ID : Any, T : Identified<ID>> invoke(name: TableName, type: KClass<T>): Table<ID, T> =
        object : Table<ID, T> {
            override fun page(where: Where<T>, order: Order<T>, after: Cursor?, limit: Int?): Rows<T> {
                require(limit == null || limit > 0) { "a page needs a limit of at least 1: $limit" }
                val filtered = matching(where)
                if (order is Order.By) {
                    require(after == null) { "Pages ordered by a field have no cursor" }
                    val byField = compareBy<Pair<Stored, Comparable<*>?>>({ it.second == null }, { it.second })
                    val sorted = filtered.map { it to order.key(it.row()) }.sortedWith(if (order.descending) byField.reversed() else byField).map { it.first }
                    return Rows((if (limit == null) sorted else sorted.take(limit)).map { it.row() }, null)
                }
                val ordered = if (order == Order.Newest) filtered.reversed() else filtered
                val matching = ordered.filter { after == null || if (order == Order.Newest) it.seq < after.value else it.seq > after.value }
                val taken = if (limit == null) matching else matching.take(limit)
                val next = if (limit != null && matching.size > limit) Cursor.of(taken.last().seq) else null
                return Rows(taken.map { it.row() }, next)
            }

            override fun count(where: Where<T>) = matching(where).size

            override fun get(id: ID) = stored().firstOrNull { it.row().id == id }?.row()

            override fun insert(row: T) = write { rows ->
                check(rows.none { it.row().id == row.id }) { "$name already has a row ${row.id} for $tenant" }
                rows + Stored((rows.lastOrNull()?.seq ?: 0) + 1, row)
            }

            override fun update(id: ID, change: (T) -> T) =
                write { rows -> rows.map { if (it.row().id == id) Stored(it.seq, change(it.row())) else it } }

            override fun delete(id: ID) = write { rows -> rows.filterNot { it.row().id == id } }

            private fun stored() = tx.state[tenant].orEmpty()[name].orEmpty()

            private fun matching(where: Where<T>) = where.filterable().let { checked -> stored().filter { checked.matches(it.row()) } }

            @Suppress("UNCHECKED_CAST")
            private fun Stored.row() = row as T

            private fun write(change: (List<Stored>) -> List<Stored>) {
                tx.state += tenant to (tx.state[tenant].orEmpty() + (name to change(stored())))
            }
        }
}
