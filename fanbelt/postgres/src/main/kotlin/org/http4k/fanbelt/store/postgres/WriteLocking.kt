/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * How Postgres writes lock and the isolation they run at, which go together: locking less than the tenant needs
 * TRANSACTION_SERIALIZABLE, or concurrent writes lose updates.
 */
interface WriteLocking {
    val isolation: Int
    fun lock(connection: Connection, tenant: TenantId, key: LockKey?)
}
