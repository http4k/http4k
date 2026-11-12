/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

interface SqlTables : Tables {
    fun <R> sql(block: (TenantConnection) -> R): R
}

class TenantConnection(val tenant: TenantId, val connection: Connection)

fun <R> Tables.sql(block: (TenantConnection) -> R): R = when (this) {
    is SqlTables -> sql(block)
    else -> error("${this::class.simpleName} has no SQL connection")
}
