package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * How Postgres writes lock, and the isolation they run at.
 */
interface WriteLocking {
    val isolation: Int
    fun lock(connection: Connection, tenant: TenantId, key: LockKey?)
}
