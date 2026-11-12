/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class TenantSqlContract {
    abstract val storage: DocumentStorage

    abstract fun add(tables: Tables, id: String, text: String)
    abstract fun tag(tables: Tables, note: String, tag: String)
    abstract fun texts(tables: Tables): List<String>
    abstract fun tagged(tables: Tables, tag: String): List<String>
    abstract fun taggedViaSubquery(tables: Tables, tag: String): List<String>
    abstract fun rename(tables: Tables, id: String, text: String): Int
    abstract fun remove(tables: Tables, id: String): Int

    private val mine = TenantId.of("mine-${System.nanoTime()}")
    private val theirs = TenantId.of("theirs-${System.nanoTime()}")

    private fun <R> on(tenant: TenantId, block: (Tables) -> R) = storage.write { block(it.tables(tenant)) }

    @BeforeEach
    fun provision() {
        on(mine) { tables ->
            tables.sql {
                it.connection.createStatement().use { statement ->
                    statement.execute("create table if not exists hatch_notes (tenant varchar(100) not null, id varchar(100) not null, text varchar(255) not null, primary key (tenant, id))")
                    statement.execute("create table if not exists hatch_tags (tenant varchar(100) not null, note varchar(100) not null, tag varchar(100) not null, primary key (tenant, note, tag))")
                }
            }
        }
    }

    @Test
    fun `reads only see the tenant's own rows`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) { add(it, "n1", "theirs") }

        assertEquals(listOf("mine"), on(mine) { texts(it) })
        assertEquals(listOf("theirs"), on(theirs) { texts(it) })
    }

    @Test
    fun `joins are filtered on every side`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) {
            add(it, "n1", "theirs")
            tag(it, "n1", "urgent")
        }

        assertEquals(emptyList<String>(), on(mine) { tagged(it, "urgent") })
        assertEquals(listOf("theirs"), on(theirs) { tagged(it, "urgent") })
    }

    @Test
    fun `subqueries are filtered too`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) {
            add(it, "n1", "theirs")
            tag(it, "n1", "urgent")
        }

        assertEquals(emptyList<String>(), on(mine) { taggedViaSubquery(it, "urgent") })
        assertEquals(listOf("theirs"), on(theirs) { taggedViaSubquery(it, "urgent") })
    }

    @Test
    fun `updates and deletes only touch the tenant's own rows`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) { add(it, "n1", "theirs") }

        assertEquals(1, on(mine) { rename(it, "n1", "renamed") })
        assertEquals(listOf("theirs"), on(theirs) { texts(it) })

        assertEquals(1, on(mine) { remove(it, "n1") })
        assertEquals(listOf("theirs"), on(theirs) { texts(it) })
    }

    @Test
    fun `inserts are stamped with the tenant`() {
        on(mine) { add(it, "n1", mine.value) }

        val stamped = on(mine) { tables ->
            tables.sql {
                it.connection.prepareStatement("select tenant from hatch_notes where text = ?").use { statement ->
                    statement.setString(1, mine.value)
                    statement.executeQuery().use { rows -> generateSequence { if (rows.next()) rows.getString(1) else null }.toSet() }
                }
            }
        }
        assertEquals(setOf(mine.value), stamped)
    }
}
