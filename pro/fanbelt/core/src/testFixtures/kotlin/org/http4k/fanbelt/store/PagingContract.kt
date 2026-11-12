/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

abstract class PagingContract {
    abstract val storage: DocumentStorage

    private fun newTenant() = TenantId.of("t-${System.nanoTime()}")

    private fun <R> on(tenant: TenantId, block: (Table<String, Note>) -> R) = storage.write { block(it.tables(tenant)(notes)) }

    open fun seeded(): TenantId {
        val tenant = newTenant()
        on(tenant) { table ->
            (1..7).forEach { table.insert(Note("n$it", if (it % 2 == 0) "even" else "odd", if (it == 7) null else "a${it % 3}", it)) }
        }
        return tenant
    }

    @Test
    fun `an id is unique within a tenant's table, like a primary key`() {
        val tenant = seeded()
        assertThrows<Exception> { on(tenant) { it.insert(Note("n1", "odd", null, 99)) } }
        Assertions.assertEquals(7, on(tenant) { it.page().items.size })
        on(newTenant()) { it.insert(Note("n1", "odd", null, 1)) }
    }

    @Test
    fun `a page needs a limit of at least 1`() {
        val tenant = seeded()

        val refused = assertThrows<IllegalArgumentException> { on(tenant) { it.page(limit = 0) } }

        Assertions.assertEquals("a page needs a limit of at least 1: 0", refused.message)
    }

    @Test
    fun `first is the first matching row in order, or null`() {
        val tenant = seeded()
        Assertions.assertEquals("n1", on(tenant) { it.first(where = Note::topic eq "odd")?.id })
        Assertions.assertEquals("n6", on(tenant) { it.first(where = Note::topic eq "even", order = Order.Newest)?.id })
        Assertions.assertEquals(null, on(tenant) { it.first(where = Note::topic eq "none") })
    }

    @Test
    fun `filters run on string fields and combine`() {
        val tenant = seeded()
        Assertions.assertEquals(
            listOf("n1", "n3", "n5", "n7"),
            on(tenant) { it.page(where = Note::topic eq "odd").map(Note::id) })
        Assertions.assertEquals(
            listOf("n1"),
            on(tenant) { it.page(where = (Note::topic eq "odd") and (Note::author eq "a1")).map(Note::id) })
        Assertions.assertEquals(listOf("n7"), on(tenant) { it.page(where = Note::author eq null).map(Note::id) })
    }

    @Test
    fun `filters can reach inside a composite key`() {
        val tenant = seeded()
        storage.write { tenants ->
            val table = tenants.tables(tenant)<Tag, Tagged>(tagged)
            (1..4).forEach { n -> table.insert(Tagged(Tag(if (n % 2 == 0) "even" else "odd", n), n)) }
        }
        Assertions.assertEquals(
            listOf("odd-1", "odd-3"),
            storage.read { it.tables(tenant)<Tag, Tagged>(tagged).page(where = (Tagged::id / Tag::topic) eq "odd").map { row -> row.id.toString() } })
    }

    @Test
    fun `filters can exclude a value or match one of several`() {
        val tenant = seeded()
        Assertions.assertEquals(listOf("n2", "n3", "n5", "n6", "n7"), on(tenant) { it.page(where = Note::author neq "a1").map(Note::id) })
        Assertions.assertEquals(listOf("n2", "n3", "n5", "n6"), on(tenant) { it.page(where = Note::author oneOf listOf("a0", "a2")).map(Note::id) })
        Assertions.assertEquals(emptyList<Note>(), on(tenant) { it.page(where = Note::author oneOf emptyList()).items })
    }

    @Test
    fun `count matches without loading rows`() {
        val tenant = seeded()
        Assertions.assertEquals(7, on(tenant) { it.count() })
        Assertions.assertEquals(4, on(tenant) { it.count(Note::topic eq "odd") })
        Assertions.assertEquals(0, on(newTenant()) { it.count() })
    }

    @Test
    fun `pages can be ordered by a field, missing values last, with a limit but no cursor`() {
        val tenant = seeded()
        Assertions.assertEquals(listOf("n7", "n6", "n5"), on(tenant) { it.page(order = descending(Note::size), limit = 3).map(Note::id) })
        Assertions.assertEquals(listOf("n3", "n6", "n1", "n4", "n2", "n5", "n7"), on(tenant) { it.page(order = ascending(Note::author)).map(Note::id) })
        Assertions.assertEquals("n7", on(tenant) { it.first(order = descending(Note::author))?.id })
        Assertions.assertNull(on(tenant) { it.page(order = ascending(Note::size), limit = 2).next })
        assertThrows<IllegalArgumentException> { on(tenant) { it.page(order = ascending(Note::size), after = Cursor.of(1)) } }
    }

    @Test
    fun `cursors page newest first and stay stable across inserts and deletes`() {
        val tenant = seeded()
        val first = on(tenant) { it.page(order = Order.Newest, limit = 3) }
        Assertions.assertEquals(listOf("n7", "n6", "n5"), first.map(Note::id))

        on(tenant) {
            it.insert(Note("n8", "even", "a2", 8))
            it.delete("n4")
        }

        val second = on(tenant) { it.page(order = Order.Newest, after = first.next, limit = 3) }
        Assertions.assertEquals(listOf("n3", "n2", "n1"), second.map(Note::id))
        Assertions.assertNull(second.next)
    }

    @Test
    fun `no limit returns everything and no cursor, and tenants never mix`() {
        val tenant = seeded()
        val everything = on(tenant) { it.page() }
        Assertions.assertEquals((1..7).map { "n$it" }, everything.map(Note::id))
        Assertions.assertNull(everything.next)
        Assertions.assertEquals(emptyList<Note>(), on(newTenant()) { it.page().items })
    }

    @Test
    fun `a write belongs to one tenant, though a read can see several`() {
        val one = newTenant()
        val two = newTenant()

        assertThrows<IllegalStateException> {
            storage.write { tenants ->
                tenants.tables(one)<String, Note>(notes).insert(Note("a", "x", null, 1))
                tenants.tables(two)<String, Note>(notes).insert(Note("b", "x", null, 1))
            }
        }

        Assertions.assertEquals(listOf(0, 0), storage.read { tenants -> listOf(one, two).map { tenants.tables(it)<String, Note>(notes).count() } })
    }

    @Test
    fun `having finds only the tenants with matching rows`() {
        val tenant = seeded()
        val other = newTenant()
        on(other) { it.insert(Note("x", "rare", null, 1)) }
        val withRare = storage.write { it.having(notes, Note::topic eq "rare") }
        Assertions.assertEquals(true, other in withRare)
        Assertions.assertEquals(false, tenant in withRare)
    }

    @Test
    fun `contains matches part of a string field, whatever the ascii case, and combines`() {
        val tenant = seeded()
        on(tenant) { table ->
            table.insert(Note("l1", "Lunch at noon", "zed", 8))
            table.insert(Note("l2", "who wants LUNCH", "yak", 9))
            table.insert(Note("l3", "something else", "zed", 10))
            table.insert(Note("l4", "lunch again", null, 11))
        }

        Assertions.assertEquals(listOf("l1", "l2", "l4"), on(tenant) { it.page(where = Note::topic contains "lunch").map(Note::id) })
        Assertions.assertEquals(listOf("l1"), on(tenant) { it.page(where = (Note::topic contains "LUNCH") and (Note::author eq "zed")).map(Note::id) })
        Assertions.assertEquals(listOf("l4"), on(tenant) { it.page(where = (Note::topic contains "lun") and (Note::author eq null)).map(Note::id) })
        Assertions.assertEquals(2, on(tenant) { it.count(Note::author contains "ZE") })
    }

    @Test
    fun `contains doesn't match a missing field`() {
        val tenant = seeded()
        Assertions.assertEquals(listOf("n1", "n2", "n3", "n4", "n5", "n6"), on(tenant) { it.page(where = Note::author contains "a").map(Note::id) })
    }

    @Test
    fun `contains treats wildcards as plain text`() {
        val tenant = seeded()
        on(tenant) { table ->
            table.insert(Note("w1", "100% done", null, 8))
            table.insert(Note("w2", "100 done", null, 9))
            table.insert(Note("w3", "snake_case", null, 10))
            table.insert(Note("w4", "snakeXcase", null, 11))
            table.insert(Note("w5", "back\\slash", null, 12))
        }

        Assertions.assertEquals(listOf("w1"), on(tenant) { it.page(where = Note::topic contains "0%").map(Note::id) })
        Assertions.assertEquals(listOf("w3"), on(tenant) { it.page(where = Note::topic contains "e_c").map(Note::id) })
        Assertions.assertEquals(listOf("w5"), on(tenant) { it.page(where = Note::topic contains "k\\s").map(Note::id) })
    }

    @Test
    fun `contains finds text outside ascii when the case is the same`() {
        val tenant = seeded()
        on(tenant) { it.insert(Note("c1", "un café", null, 8)) }

        Assertions.assertEquals(listOf("c1"), on(tenant) { it.page(where = Note::topic contains "café").map(Note::id) })
    }

    @Test
    fun `having finds the tenants whose rows contain the text`() {
        val tenant = seeded()
        val other = seeded()
        on(tenant) { it.insert(Note("r1", "a rare find", null, 8)) }

        val withRare = storage.read { it.having(notes, Note::topic contains "RARE FIND") }
        Assertions.assertEquals(true, tenant in withRare)
        Assertions.assertEquals(false, other in withRare)
    }

    @Test
    fun `filtering on a field that isn't a JSON string fails loudly`() {
        val tenant = seeded()
        assertThrows<IllegalArgumentException> { on(tenant) { it.page(where = Note::size eq 3) } }
    }
}
