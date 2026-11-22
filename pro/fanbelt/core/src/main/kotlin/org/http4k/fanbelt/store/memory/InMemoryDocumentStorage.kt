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
import org.http4k.fanbelt.store.Cursor
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.InProcessLocks
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.Order
import org.http4k.fanbelt.store.Rows
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.Where
import org.http4k.fanbelt.store.sameId
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

/**
 * Document storage in memory, for one node and for tests.
 */
fun InMemoryDocumentStorage(
    retryPolicy: RetryPolicy = increasingBackoff(Duration.ofMillis(1)).withAdditiveJitter().maxAttempts(50),
): DocumentStorage {
    val seqs = AtomicLong()
    return InMemoryStorage(InMemoryTransactor(emptyMap(), { tx -> InMemoryTenants(tx, seqs) }, retryPolicy))
}

private class InMemoryStorage(private val transactor: Transactional<InMemoryTenants>) : DocumentStorage {
    override val locks = InProcessLocks()
    override fun <R> write(work: (Tenants) -> R) = transactor.perform { work(OneTenant(it)) }
    override fun <R> read(work: (Tenants) -> R) = transactor.perform(work)
}

private class OneTenant(private val tenants: InMemoryTenants) : Tenants by tenants {
    private var owner: TenantId? = null

    override fun tables(tenant: TenantId, key: LockKey?): Tables {
        check(owner == null || owner == tenant) { "A write belongs to one tenant: it has $owner, so it can't also write $tenant" }
        owner = tenant
        return tenants.writing(tenant)
    }
}

private class Stored(val seq: Long, val row: Identified<*>)

private typealias TenantRows = Map<TenantId, Map<TableName, List<Stored>>>

private class InMemoryTenants(private val tx: InMemoryTransaction<TenantRows>, private val seqs: AtomicLong) : Tenants {
    override fun tables(tenant: TenantId, key: LockKey?): Tables = InMemoryTables(tx, seqs, tenant, writable = false)

    fun writing(tenant: TenantId): Tables = InMemoryTables(tx, seqs, tenant, writable = true)

    @Suppress("UNCHECKED_CAST")
    override fun <T> having(name: TableName, where: Where<T>) =
        tx.state.filterValues { tables -> tables[name].orEmpty().any { where.matches(it.row as T) } }.keys
}

private class InMemoryTables(
    private val tx: InMemoryTransaction<TenantRows>,
    private val seqs: AtomicLong,
    private val tenant: TenantId,
    private val writable: Boolean,
) : Tables {
    override fun <ID : Value<*>, T : Identified<ID>> invoke(name: TableName, type: KClass<T>): Table<ID, T> =
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
                rows + Stored(seqs.incrementAndGet(), row)
            }

            override fun update(id: ID, change: (T) -> T) =
                write { rows -> rows.map { if (it.row().id == id) Stored(it.seq, change(it.row()).sameId(id)) else it } }

            override fun delete(id: ID) = write { rows -> rows.filterNot { it.row().id == id } }

            private fun stored() = tx.state[tenant].orEmpty()[name].orEmpty()

            private fun matching(where: Where<T>) = stored().filter { where.matches(it.row()) }

            @Suppress("UNCHECKED_CAST")
            private fun Stored.row() = row as T

            private fun write(change: (List<Stored>) -> List<Stored>) {
                check(writable) { "A read is read-only: it can't write $name for $tenant" }
                tx.state += tenant to (tx.state[tenant].orEmpty() + (name to change(stored())))
            }
        }
}
