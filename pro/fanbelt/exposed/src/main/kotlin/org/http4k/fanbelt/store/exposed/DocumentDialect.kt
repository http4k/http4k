/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import org.jetbrains.exposed.v1.json.jsonb
import java.sql.Connection
import java.sql.Connection.TRANSACTION_READ_COMMITTED
import java.sql.Connection.TRANSACTION_REPEATABLE_READ
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

interface DocumentDialect {
    fun schema(name: TableName): DocumentSchema
    fun text(doc: Column<String>, path: List<String>): Expression<String>
    fun value(doc: Column<String>, path: List<String>): Expression<String>
    fun nextSeq(docs: DocumentSchema, owned: Op<Boolean>): Long?
    fun bind(connection: Connection, tenant: TenantId)
    fun lock(connection: Connection, tenant: TenantId)
    fun retryable(e: Exception): Boolean
    fun write(db: Database): JdbcTransaction = db.transactionManager.newTransaction(TRANSACTION_SERIALIZABLE, false, null)
    fun read(db: Database): JdbcTransaction
}

object PostgresDocuments : DocumentDialect {
    private val schemas = ConcurrentHashMap<TableName, DocumentSchema>()

    override fun schema(name: TableName): DocumentSchema = schemas.computeIfAbsent(name) { DocumentSchema(it, { long(it).autoIncrement() }, { column -> jsonb<String>(column, { it }, { it }) }) }

    override fun text(doc: Column<String>, path: List<String>) = Sql("(", doc, path.dropLast(1).joinToString("") { " -> '$it'" }, " ->> '${path.last()}')")

    override fun value(doc: Column<String>, path: List<String>) = Sql("(", doc, path.joinToString("") { " -> '$it'" }, ")")

    override fun nextSeq(docs: DocumentSchema, owned: Op<Boolean>) = null

    override fun bind(connection: Connection, tenant: TenantId) {
        connection.prepareStatement("select set_config('fanbelt.tenant', ?, true)").use {
            it.setString(1, tenant.value)
            it.execute()
        }
    }

    override fun lock(connection: Connection, tenant: TenantId) {
        connection.prepareStatement("select pg_advisory_xact_lock(hashtext(?))").use {
            it.setString(1, tenant.value)
            it.execute()
        }
    }

    override fun retryable(e: Exception) = e is SQLException && e.sqlState in setOf("40001", "40P01")

    override fun write(db: Database) = db.transactionManager.newTransaction(TRANSACTION_READ_COMMITTED, false, null)

    override fun read(db: Database) = db.transactionManager.newTransaction(TRANSACTION_REPEATABLE_READ, true, null)
}

class Sql(private vararg val parts: Any) : Expression<String>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = parts.forEach {
        when (it) {
            is Expression<*> -> queryBuilder.append(it)
            else -> queryBuilder.append(it.toString())
        }
    }
}
