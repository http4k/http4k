package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId

/**
 * The tenants in a transaction.
 */
interface Tenants {
    fun tables(tenant: TenantId, key: LockKey? = null): Tables

    fun <T> having(name: TableName, where: Where<T>): Set<TenantId>
}
