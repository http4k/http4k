package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * Tables in a SQL database.
 */
interface SqlTables : Tables {
    fun <R> sql(block: (TenantConnection) -> R): R
}

/**
 * The transaction's JDBC connection and its tenant.
 */
class TenantConnection(val tenant: TenantId, val connection: Connection)

fun <R> Tables.sql(block: (TenantConnection) -> R): R = when (this) {
    is SqlTables -> sql(block)
    else -> error("${this::class.simpleName} has no SQL connection")
}
