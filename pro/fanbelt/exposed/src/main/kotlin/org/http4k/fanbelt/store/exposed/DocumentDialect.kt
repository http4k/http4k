/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.Locks
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.sql.Connection

/**
 * What document storage does differently per database.
 */
interface DocumentDialect {
    fun schema(name: TableName): DocumentSchema
    fun text(doc: Column<String>, path: List<String>): Expression<String>
    fun value(doc: Column<String>, path: List<String>): Expression<String>
    fun sorted(doc: Column<String>, path: List<String>, type: Class<*>): Expression<String>
    fun nextSeq(docs: DocumentSchema, owned: Op<Boolean>): Long?
    fun bind(connection: Connection, tenant: TenantId)
    fun lock(connection: Connection, tenant: TenantId, key: LockKey?)
    fun retryable(e: Exception): Boolean
    fun write(db: Database): JdbcTransaction
    fun read(db: Database): JdbcTransaction
    fun locks(db: Database): Locks
}

/**
 * A raw SQL expression built from text and other expressions.
 */
class Sql(private vararg val parts: Any) : Expression<String>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = parts.forEach {
        when (it) {
            is Expression<*> -> queryBuilder.append(it)
            else -> queryBuilder.append(it.toString())
        }
    }
}
