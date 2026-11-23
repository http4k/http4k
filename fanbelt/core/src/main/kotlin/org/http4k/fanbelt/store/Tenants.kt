package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId

/**
 * The tenants in a transaction. having is the only cross-tenant read. A write may pass the key it acts on, which some
 * storage locks instead of the whole tenant.
 */
interface Tenants {
    fun tables(tenant: TenantId, key: LockKey? = null): Tables

    fun <T> having(name: TableName, where: Where<T>): Set<TenantId>
}
