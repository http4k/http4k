package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.exposed.DocumentDialect
import org.http4k.fanbelt.store.exposed.DocumentSchema
import org.http4k.fanbelt.store.exposed.Sql
import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import org.jetbrains.exposed.v1.json.jsonb
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

internal class Postgres(private val locking: WriteLocking) : DocumentDialect {
    override fun schema(name: TableName): DocumentSchema = schemas.computeIfAbsent(name) { DocumentSchema(it, { long(it).autoIncrement() }, { column -> jsonb<String>(column, { it }, { it }) }) }

    override fun text(doc: Column<String>, path: List<String>) = Sql("(", doc, path.dropLast(1).joinToString("") { " -> '$it'" }, " ->> '${path.last()}')")

    override fun value(doc: Column<String>, path: List<String>) = Sql("(", doc, path.joinToString("") { " -> '$it'" }, ")")

    override fun sorted(doc: Column<String>, path: List<String>, type: Class<*>) = comparable(type, text(doc, path)) ?: value(doc, path)

    override fun bound(doc: Column<String>, json: String, value: Any, type: Class<*>) =
        comparable(type, QueryParameter(value.toString(), TextColumnType())) ?: Sql(QueryParameter(json, doc.columnType))

    private fun comparable(type: Class<*>, text: Expression<*>) = when (type) {
        Instant::class.java, OffsetDateTime::class.java -> Sql("CAST(", text, " AS timestamptz)")
        String::class.java -> Sql("(", text, ") COLLATE \"C\"")
        else -> null
    }

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
