/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId

/**
 * The tenants in a transaction. having is the only cross-tenant read.
 */
interface Tenants {
    fun tables(tenant: TenantId): Tables

    fun <T> having(name: TableName, where: Where<T>): Set<TenantId>
}
