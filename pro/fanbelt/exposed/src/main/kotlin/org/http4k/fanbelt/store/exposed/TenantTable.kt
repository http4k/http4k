/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction

internal val Owner = Key<TenantId>()

/**
 * An app's own Exposed table with a tenant column, filtered and stamped by TenantSql.
 */
abstract class TenantTable(name: String) : Table(name) {
    val tenant = varchar("tenant", 100)

    override fun describe(s: Transaction, queryBuilder: QueryBuilder) {
        val owner = s.getUserData(Owner)
        if (owner == null || queryBuilder.toString().endsWith("UPDATE ")) return super.describe(s, queryBuilder)
        queryBuilder.append("(SELECT * FROM ${s.identity(this)} WHERE ${s.identity(tenant)} = ")
        queryBuilder.registerArgument(tenant, owner.value)
        queryBuilder.append(") ${s.identity(this)}")
    }
}
