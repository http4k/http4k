package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * Each write locks its whole tenant, so a tenant's writes run one at a time at READ COMMITTED.
 */
object TenantLocking : WriteLocking {
    override val isolation = Connection.TRANSACTION_READ_COMMITTED

    override fun lock(connection: Connection, tenant: TenantId, key: LockKey?) =
        connection.statement("select pg_advisory_xact_lock(hashtext(?))", tenant.value)
}
