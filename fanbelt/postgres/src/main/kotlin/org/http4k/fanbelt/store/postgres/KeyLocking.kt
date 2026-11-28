package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * Postgres writes that lock their key, so writes to different keys of one tenant run side by side.
 */
object KeyLocking : WriteLocking {
    override val isolation = Connection.TRANSACTION_SERIALIZABLE

    override fun lock(connection: Connection, tenant: TenantId, key: LockKey?) = when (key) {
        null -> TenantLocking.lock(connection, tenant, null)

        else -> {
            connection.statement("select pg_advisory_xact_lock_shared(hashtext(?))", tenant.value)
            connection.statement("select pg_advisory_xact_lock(hashtext(?), hashtext(?))", tenant.value, key.value)
        }
    }
}
