/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import dev.forkhandles.tx.RetryPolicy
import dev.forkhandles.tx.Transactor
import dev.forkhandles.tx.increasingBackoff
import dev.forkhandles.tx.maxAttempts
import dev.forkhandles.tx.withAdditiveJitter
import org.http4k.fanbelt.store.Among
import org.http4k.fanbelt.store.Clause
import org.http4k.fanbelt.store.Containing
import org.http4k.fanbelt.store.Cursor
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Order
import org.http4k.fanbelt.store.Rows
import org.http4k.fanbelt.store.SqlTables
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.TenantConnection
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.Where
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.AutoMarshalling
import org.http4k.format.AutoMarshallingJson
import org.jetbrains.exposed.v1.core.EqOp
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.IsNullOp
import org.jetbrains.exposed.v1.core.LikeEscapeOp
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder.ASC
import org.jetbrains.exposed.v1.core.SortOrder.ASC_NULLS_LAST
import org.jetbrains.exposed.v1.core.SortOrder.DESC
import org.jetbrains.exposed.v1.core.SortOrder.DESC_NULLS_FIRST
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.compoundOr
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.core.transactions.withThreadLocalTransaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.Connection
import java.time.Duration
import kotlin.reflect.KClass

fun ExposedDocumentStorage(
    db: Database,
    json: AutoMarshallingJson<*>,
    dialect: DocumentDialect,
    retryPolicy: RetryPolicy = increasingBackoff(Duration.ofMillis(50)).withAdditiveJitter().maxAttempts(5),
    readDb: Database = db,
): DocumentStorage = ExposedStorage(
    writes = ExposedTransactor(db, json, retryPolicy, dialect, dialect::write) { OneTenant(dialect) },
    reads = ExposedTransactor(readDb, json, retryPolicy, dialect, dialect::read) { AnyTenant },
)

private fun interface Claim {
    fun claim(tx: JdbcTransaction, tenant: TenantId)
}

private val AnyTenant = Claim { _, _ -> }

private class OneTenant(private val dialect: DocumentDialect) : Claim {
    private var owner: TenantId? = null

    override fun claim(tx: JdbcTransaction, tenant: TenantId) {
        check(owner == null || owner == tenant) { "A write belongs to one tenant: it has $owner, so it can't also write $tenant" }
        owner = tenant
        tx.bound { dialect.lock(tx.jdbc(), tenant) }
    }
}

private class ExposedStorage(private val writes: ExposedTransactor, private val reads: ExposedTransactor) : DocumentStorage {
    override fun <R> write(work: (Tenants) -> R) = writes.perform(work)
    override fun <R> read(work: (Tenants) -> R) = reads.perform(work)
}

private class ExposedTransactor(
    private val db: Database,
    private val json: AutoMarshalling,
    private val retryPolicy: RetryPolicy,
    private val dialect: DocumentDialect,
    private val open: (Database) -> JdbcTransaction,
    private val claims: () -> Claim,
) : Transactor<JdbcTransaction, Tenants>() {
    override fun createResource() = open(db)
    override fun configureResource(resource: JdbcTransaction) = Unit
    override fun destroyResource(resource: JdbcTransaction) = resource.bound { resource.close() }

    override fun createApi(resource: JdbcTransaction): Tenants = ExposedTenants(resource, json, dialect, claims())
    override fun startTransaction(resource: JdbcTransaction) = Unit
    override fun commitTransaction(resource: JdbcTransaction) = resource.commit()
    override fun rollbackTransaction(resource: JdbcTransaction) = resource.rollback()
    override fun canRetry(e: Exception) = dialect.retryable(e)
    override fun retryBackoff(attempt: Int) = retryPolicy(attempt)
}

private class ExposedTenants(
    private val tx: JdbcTransaction,
    private val json: AutoMarshalling,
    private val dialect: DocumentDialect,
    private val claim: Claim,
) : Tenants {
    override fun tables(tenant: TenantId): Tables {
        claim.claim(tx, tenant)
        return ExposedTables(tx, json, tenant, dialect)
    }

    override fun <T> having(name: TableName, where: Where<T>): Set<TenantId> = tx.bound {
        val docs = dialect.schema(name)
        docs.select(docs.tenant)
            .where(where.clauses.fold(Op.TRUE as Op<Boolean>) { conditions, clause -> conditions and clause.toOp(docs, json, dialect) })
            .withDistinct()
            .map { TenantId.of(it[docs.tenant]) }
            .toSet()
    }
}

private class ExposedTables(
    private val resource: JdbcTransaction,
    private val json: AutoMarshalling,
    private val tenant: TenantId,
    private val dialect: DocumentDialect,
) : SqlTables {
    override fun <ID : Any, T : Identified<ID>> invoke(name: TableName, type: KClass<T>): Table<ID, T> =
        TenantDocumentTable(resource, json, dialect, dialect.schema(name), tenant.value, type)

    override fun <R> sql(block: (TenantConnection) -> R): R = resource.bound {
        val connection = resource.jdbc()
        dialect.bind(connection, tenant)
        block(TenantConnection(tenant, connection))
    }
}

private class TenantDocumentTable<ID : Any, T : Identified<ID>>(
    private val tx: JdbcTransaction,
    private val json: AutoMarshalling,
    private val dialect: DocumentDialect,
    private val docs: DocumentSchema,
    private val owner: String,
    private val rowType: KClass<T>,
) : Table<ID, T> {

    private fun ownedRows() = docs.tenant eq owner

    private fun row(id: ID) = ownedRows() and (docs.id eq id.toString())

    override fun page(where: Where<T>, order: Order<T>, after: Cursor?, limit: Int?): Rows<T> = tx.bound {
        require(limit == null || limit > 0) { "a page needs a limit of at least 1: $limit" }
        val filtered = filtered(where)
        if (order is Order.By) {
            require(after == null) { "Pages ordered by a field have no cursor" }
            val sort = if (order.descending) DESC_NULLS_FIRST else ASC_NULLS_LAST
            val query = docs.selectAll().where(filtered).orderBy(dialect.value(docs.doc, listOf(order.field.name)) to sort, docs.seq to ASC)
            return@bound Rows((if (limit == null) query else query.limit(limit)).map { json.asA(it[docs.doc], rowType) }, null)
        }
        val bounded = when {
            after == null -> filtered
            order == Order.Newest -> filtered and (docs.seq less after.value)
            else -> filtered and (docs.seq greater after.value)
        }
        val query = docs.selectAll().where(bounded).orderBy(docs.seq, if (order == Order.Newest) DESC else ASC)
        val rows = (if (limit == null) query else query.limit(limit + 1)).toList()
        val taken = if (limit == null) rows else rows.take(limit)
        val next = if (limit != null && rows.size > limit) Cursor.of(taken.last()[docs.seq]) else null
        Rows(taken.map { json.asA(it[docs.doc], rowType) }, next)
    }

    override fun count(where: Where<T>) = tx.bound { docs.selectAll().where(filtered(where)).count().toInt() }

    private fun filtered(where: Where<T>) = where.clauses.fold(ownedRows()) { conditions, clause -> conditions and clause.toOp(docs, json, dialect) }

    override fun get(id: ID) = tx.bound {
        docs.selectAll().where(row(id)).singleOrNull()?.let { json.asA(it[docs.doc], rowType) }
    }

    override fun insert(row: T) {
        tx.bound {
            docs.insert {
                it[docs.tenant] = owner
                it[docs.id] = row.id.toString()
                val seq = dialect.nextSeq(docs, ownedRows())
                if (seq != null) it[docs.seq] = seq
                it[docs.doc] = json.asFormatString(row)
            }
        }
    }

    override fun update(id: ID, change: (T) -> T) {
        val current = get(id) ?: return
        tx.bound { docs.update({ row(id) }) { it[docs.doc] = json.asFormatString(change(current)) } }
    }

    override fun delete(id: ID) {
        tx.bound { docs.deleteWhere { row(id) } }
    }
}

private fun <T> Clause<T>.toOp(docs: DocumentSchema, json: AutoMarshalling, dialect: DocumentDialect): Op<Boolean> = when (this) {
    is Among -> toOp(docs, json, dialect)
    is Containing -> toOp(docs, dialect)
}

private fun <T> Containing<T>.toOp(docs: DocumentSchema, dialect: DocumentDialect): Op<Boolean> {
    val literal = LikePattern.ofLiteral(text)
    val pattern = LikePattern("%", literal.escapeChar) + literal + "%"
    return LikeEscapeOp(LowerCase(dialect.text(docs.doc, field.path)), LowerCase(stringParam(pattern.pattern)), true, pattern.escapeChar)
}

private fun <T> Among<T>.toOp(docs: DocumentSchema, json: AutoMarshalling, dialect: DocumentDialect): Op<Boolean> {
    val extracted = dialect.text(docs.doc, field.path)
    val matching = if (values.isEmpty()) Op.FALSE else values.map { if (it == null) IsNullOp(extracted) else EqOp(extracted, stringParam(it.asJsonString(field.path.joinToString("."), json))) }.compoundOr()
    return when {
        !negated -> matching
        null in values -> not(matching)
        else -> not(matching) or IsNullOp(extracted)
    }
}

private fun Any.asJsonString(field: String, json: AutoMarshalling): String {
    val encoded = json.asFormatString(this)
    require(encoded.startsWith("\"")) { "Can only filter on fields that serialise as JSON strings, but $field is $encoded" }
    return json.asA(encoded, String::class)
}

@OptIn(InternalApi::class)
private fun <R> JdbcTransaction.bound(block: () -> R): R = withThreadLocalTransaction(this, block)

private fun JdbcTransaction.jdbc(): Connection {
    val connection = connection.connection
    check(connection is Connection) { "Expected a JDBC connection but got $connection" }
    return connection
}
