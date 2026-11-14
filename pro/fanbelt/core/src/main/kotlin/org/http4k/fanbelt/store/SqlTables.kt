/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * Tables that live in a SQL database, which offer the transaction's own JDBC connection for an app's own tables.
 */
interface SqlTables : Tables {
    fun <R> sql(block: (TenantConnection) -> R): R
}

/**
 * The transaction's JDBC connection and the tenant it belongs to. On Postgres the connection is bound to the tenant, so
 * row-level security keyed on fanbelt.tenant applies.
 */
class TenantConnection(val tenant: TenantId, val connection: Connection)

/**
 * Runs a block on the operation's own JDBC connection, in its transaction, for tables the app manages itself. Fails on
 * storage that isn't SQL.
 */
fun <R> Tables.sql(block: (TenantConnection) -> R): R = when (this) {
    is SqlTables -> sql(block)
    else -> error("${this::class.simpleName} has no SQL connection")
}
