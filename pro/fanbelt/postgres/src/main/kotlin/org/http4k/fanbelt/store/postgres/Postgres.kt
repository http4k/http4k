/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.lock.postgres.PostgresLocks
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.exposed.DocumentDialect
import org.http4k.fanbelt.store.exposed.DocumentSchema
import org.http4k.fanbelt.store.exposed.Sql
import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import org.jetbrains.exposed.v1.json.jsonb
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

internal class Postgres(private val locking: WriteLocking) : DocumentDialect {
    override fun schema(name: TableName): DocumentSchema = schemas.computeIfAbsent(name) { DocumentSchema(it, { long(it).autoIncrement() }, { column -> jsonb<String>(column, { it }, { it }) }) }

    override fun text(doc: Column<String>, path: List<String>) = Sql("(", doc, path.dropLast(1).joinToString("") { " -> '$it'" }, " ->> '${path.last()}')")

    override fun value(doc: Column<String>, path: List<String>) = Sql("(", doc, path.joinToString("") { " -> '$it'" }, ")")

    override fun nextSeq(docs: DocumentSchema, owned: Op<Boolean>) = null

    override fun bind(connection: Connection, tenant: TenantId) = connection.statement("select set_config('fanbelt.tenant', ?, true)", tenant.value)

    override fun lock(connection: Connection, tenant: TenantId, key: LockKey?) = locking.lock(connection, tenant, key)

    override fun retryable(e: Exception) = e is SQLException && e.sqlState in setOf("40001", "40P01")

    override fun write(db: Database) = db.transactionManager.newTransaction(locking.isolation, false, null)

    override fun read(db: Database) = db.transactionManager.newTransaction(Connection.TRANSACTION_REPEATABLE_READ, true, null)

    override fun locks(db: Database) = PostgresLocks {
        val connection = db.connector().connection
        check(connection is Connection) { "Expected a JDBC connection but got $connection" }
        connection
    }
}

private val schemas = ConcurrentHashMap<TableName, DocumentSchema>()

internal fun Connection.statement(sql: String, vararg keys: String) = prepareStatement(sql).use { statement ->
    keys.forEachIndexed { index, key -> statement.setString(index + 1, key) }
    statement.execute()
    Unit
}
