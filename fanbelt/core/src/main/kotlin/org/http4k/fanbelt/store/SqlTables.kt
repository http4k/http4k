package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * Tables in a SQL database, giving access to the transaction's JDBC connection.
 */
interface SqlTables : Tables {
    fun <R> sql(block: (TenantConnection) -> R): R
}

/**
 * The transaction's JDBC connection and its tenant. On Postgres, row-level security on fanbelt.tenant applies.
 */
class TenantConnection(val tenant: TenantId, val connection: Connection)

/**
 * Runs a block on the transaction's JDBC connection, for the app's own tables. Fails on storage that isn't SQL.
 */
fun <R> Tables.sql(block: (TenantConnection) -> R): R = when (this) {
    is SqlTables -> sql(block)
    else -> error("${this::class.simpleName} has no SQL connection")
}
