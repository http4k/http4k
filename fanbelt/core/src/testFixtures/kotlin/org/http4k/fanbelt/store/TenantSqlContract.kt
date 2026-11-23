/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.tenancy.TenantId
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
    abstract fun renameTagged(tables: Tables, tag: String, text: String): Int
    abstract fun withTags(tables: Tables): List<Pair<String, String?>>
    abstract fun textsViaAlias(tables: Tables): List<String>
    open val filtersAliases = true
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

        assertThat(on(mine) { texts(it) }, equalTo(listOf("mine")))
        assertThat(on(theirs) { texts(it) }, equalTo(listOf("theirs")))
    }

    @Test
    fun `joins are filtered on every side`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) {
            add(it, "n1", "theirs")
            tag(it, "n1", "urgent")
        }

        assertThat(on(mine) { tagged(it, "urgent") }, equalTo(emptyList<String>()))
        assertThat(on(theirs) { tagged(it, "urgent") }, equalTo(listOf("theirs")))
    }

    @Test
    fun `subqueries are filtered too`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) {
            add(it, "n1", "theirs")
            tag(it, "n1", "urgent")
        }

        assertThat(on(mine) { taggedViaSubquery(it, "urgent") }, equalTo(emptyList<String>()))
        assertThat(on(theirs) { taggedViaSubquery(it, "urgent") }, equalTo(listOf("theirs")))
    }

    @Test
    fun `updates and deletes only touch the tenant's own rows`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) { add(it, "n1", "theirs") }

        assertThat(on(mine) { rename(it, "n1", "renamed") }, equalTo(1))
        assertThat(on(theirs) { texts(it) }, equalTo(listOf("theirs")))

        assertThat(on(mine) { remove(it, "n1") }, equalTo(1))
        assertThat(on(theirs) { texts(it) }, equalTo(listOf("theirs")))
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
        assertThat(stamped, equalTo(setOf(mine.value)))
    }

    @Test
    fun `an outer join keeps the tenant's unmatched rows and none of another tenant's matches`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) {
            add(it, "n1", "theirs")
            tag(it, "n1", "urgent")
        }

        assertThat(on(mine) { withTags(it) }, equalTo(listOf<Pair<String, String?>>("mine" to null)))
    }

    @Test
    fun `an aliased table is filtered, or refused where it can't be`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) { add(it, "n1", "theirs") }

        val seen = runCatching { on(mine) { textsViaAlias(it) } }

        if (filtersAliases) {
            assertThat(seen.getOrThrow(), equalTo(listOf("mine")))
        } else {
            assertThat(seen.exceptionOrNull() is IllegalArgumentException, equalTo(true))
        }
    }

    @Test
    fun `a subquery inside an update is filtered too`() {
        on(mine) { add(it, "n1", "mine") }
        on(theirs) {
            add(it, "n1", "theirs")
            tag(it, "n1", "urgent")
        }

        assertThat(on(mine) { renameTagged(it, "urgent", "renamed") }, equalTo(0))
        assertThat(on(mine) { texts(it) }, equalTo(listOf("mine")))
    }
}
