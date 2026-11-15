/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.tenancy.TenantId
import java.sql.Connection

/**
 * A write with a key locks that key, so writes to different keys of one tenant run side by side; one with no key locks
 * the whole tenant. Writes run SERIALIZABLE, so one that reads beyond its key is retried rather than losing an update.
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
