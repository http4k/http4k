/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.jooq

import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.sql
import org.http4k.fanbelt.tenancy.TenantId
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.QueryPart
import org.jooq.Select
import org.jooq.Table
import org.jooq.VisitContext
import org.jooq.VisitListener
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultVisitListenerProvider
import org.jooq.impl.QOM
import org.jooq.impl.TableImpl
import org.jooq.tools.jdbc.JDBCUtils
import java.util.Collections
import java.util.IdentityHashMap

fun <R> Tables.jooq(block: (DSLContext) -> R): R = sql {
    val configuration = DefaultConfiguration()
        .set(it.connection)
        .set(JDBCUtils.dialect(it.connection))
        .set(DefaultVisitListenerProvider(TenantFilter(it.tenant)))
    block(DSL.using(configuration))
}

private class TenantFilter(private val owner: TenantId) : VisitListener {
    private val rewritten = Collections.newSetFromMap(IdentityHashMap<QueryPart, Boolean>())

    override fun visitStart(context: VisitContext) {
        val part = context.queryPart()
        if (part in rewritten) return
        val replacement = rewrite(part) ?: return
        rewritten += replacement
        context.queryPart(replacement)
    }

    private fun rewrite(part: QueryPart?): QueryPart? = when (part) {
        is Select<*> -> part.`$where`(owned(part.`$where`(), part.`$from`()))
        is QOM.Update<*> -> part.`$where`(owned(part.`$where`(), listOf(part.`$table`()) + part.`$from`()))
        is QOM.Delete<*> -> part.`$where`(owned(part.`$where`(), listOf(part.`$from`())))
        is QOM.Insert<*> -> stamped(part)
        else -> null
    }

    private fun owned(where: Condition?, from: List<Table<*>>): Condition =
        DSL.and(listOfNotNull(where) + from.flatMap(::named).map { tenantOf(it).eq(owner.value) })

    private fun stamped(insert: QOM.Insert<*>): QueryPart {
        require(insert.`$select`() == null) { "Insert from a select can't be stamped with the tenant" }
        require(insert.`$columns`().none { it.name == TENANT }) { "The tenant is set for you" }
        val column = DSL.field(DSL.name(TENANT), String::class.java)
        return insert
            .`$columns`(insert.`$columns`() + column)
            .`$values`(insert.`$values`().map { DSL.row(it.fields().toList() + DSL.value(owner.value)) })
    }

    private fun named(table: Table<*>): List<Table<*>> = when (table) {
        is QOM.JoinTable<*, *> -> named(table.`$table1`()) + named(table.`$table2`())
        is QOM.TableAlias<*> -> if (table.`$table`() is TableImpl<*>) listOf(table) else emptyList()
        is TableImpl<*> -> listOf(table)
        else -> emptyList()
    }

    private fun tenantOf(table: Table<*>): Field<String> {
        val name = if (table is QOM.TableAlias<*>) table.`$alias`() else table.qualifiedName
        return DSL.field(name.append(TENANT), String::class.java)
    }
}

private const val TENANT = "tenant"
