/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.sql
import org.http4k.fanbelt.tenancy.TenantId
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Exposed queries on TenantTables, filtered to and stamped with one tenant.
 */
class TenantSql internal constructor(private val owner: TenantId) {
    fun query(build: () -> Query): List<ResultRow> = owned {
        val query = build()
        require(query.set.source.columns.none { (it.table as? Alias<*>)?.delegate is TenantTable }) {
            "An aliased TenantTable isn't filtered by tenant: use tables.jooq { }, or rely on row-level security"
        }
        query.toList()
    }

    fun <T : TenantTable> insert(table: T, body: T.(InsertStatement<Number>) -> Unit) {
        table.insert {
            it[table.tenant] = owner.value
            body(it)
        }
    }

    fun <T : TenantTable> update(table: T, where: () -> Op<Boolean>, body: T.(UpdateStatement) -> Unit) =
        owned { table.update({ (table.tenant eq owner.value) and where() }, body = body) }

    fun <T : TenantTable> delete(table: T, where: () -> Op<Boolean>) =
        owned { table.deleteWhere { (table.tenant eq owner.value) and where() } }

    private fun <R> owned(block: () -> R): R {
        val transaction = TransactionManager.current()
        transaction.putUserData(Owner, owner)
        try {
            return block()
        } finally {
            transaction.removeUserData(Owner)
        }
    }
}

/**
 * Runs Exposed on the app's TenantTables in the operation's transaction.
 */
fun <R> Tables.exposed(block: TenantSql.() -> R): R = sql { TenantSql(it.tenant).block() }
