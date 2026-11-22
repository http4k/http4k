/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.throws
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

abstract class PagingContract {
    abstract val storage: DocumentStorage

    private fun newTenant() = TenantId.of("t-${System.nanoTime()}")

    private fun <R> on(tenant: TenantId, block: (Table<RowId, Note>) -> R) = storage.write { block(it.tables(tenant)(notes)) }

    open fun seeded(): TenantId {
        val tenant = newTenant()
        on(tenant) { table ->
            (1..7).forEach { table.insert(Note(RowId.of("n$it"), if (it % 2 == 0) "even" else "odd", if (it == 7) null else "a${it % 3}", it)) }
        }
        return tenant
    }

    @Test
    fun `an id is unique within a tenant's table, like a primary key`() {
        val tenant = seeded()
        assertThat({ on(tenant) { it.insert(Note(RowId.of("n1"), "odd", null, 99)) } }, throws<Exception>())
        assertThat(on(tenant) { it.page().items.size }, equalTo(7))
        on(newTenant()) { it.insert(Note(RowId.of("n1"), "odd", null, 1)) }
    }

    @Test
    fun `an update can't change a row's id, and changes nothing when it tries`() {
        val tenant = seeded()

        assertThat(
            { on(tenant) { table -> table.update(RowId.of("n1")) { it.copy(id = RowId.of("n9"), size = 99) } } },
            throws<IllegalArgumentException>(has(Throwable::message, equalTo("An update can't change a row's id (n1 to n9): delete it and insert another")))
        )
        assertThat(on(tenant) { it.get(RowId.of("n1"))?.size }, equalTo(1))
        assertThat(on(tenant) { it.get(RowId.of("n9")) }, absent())
    }

    @Test
    fun `ids that print alike are still different rows`() {
        val tenant = newTenant()
        storage.write { tenants ->
            val table = tenants.tables(tenant)<Hushed, Whisper>(whispers)
            table.insert(Whisper(Hushed.of("a"), "first"))
            table.insert(Whisper(Hushed.of("b"), "second"))
        }

        assertThat(storage.read { it.tables(tenant)<Hushed, Whisper>(whispers).get(Hushed.of("b"))?.text }, equalTo("second"))
    }

    @Test
    fun `a page needs a limit of at least 1`() {
        val tenant = seeded()

        assertThat({ on(tenant) { it.page(limit = 0) } }, throws<IllegalArgumentException>(has(Throwable::message, equalTo("a page needs a limit of at least 1: 0"))))
    }

    @Test
    fun `first is the first matching row in order, or null`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.first(where = Note::topic eq "odd")?.id }, equalTo(RowId.of("n1")))
        assertThat(on(tenant) { it.first(where = Note::topic eq "even", order = Order.Newest)?.id }, equalTo(RowId.of("n6")))
        assertThat(on(tenant) { it.first(where = Note::topic eq "none") }, equalTo(null))
    }

    @Test
    fun `filters run on string fields and combine`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.page(where = Note::topic eq "odd").map(Note::id) }, equalTo(ids("n1", "n3", "n5", "n7")))
        assertThat(on(tenant) { it.page(where = (Note::topic eq "odd") and (Note::author eq "a1")).map(Note::id) }, equalTo(ids("n1")))
        assertThat(on(tenant) { it.page(where = Note::author eq null).map(Note::id) }, equalTo(ids("n7")))
    }

    @Test
    fun `filters can reach inside a composite field`() {
        val tenant = seeded()
        storage.write { tenants ->
            val table = tenants.tables(tenant)<RowId, Tagged>(tagged)
            (1..4).forEach { n -> table.insert(Tagged(RowId.of("t$n"), Tag(if (n % 2 == 0) "even" else "odd", n), n)) }
        }
        assertThat(storage.read { it.tables(tenant)<RowId, Tagged>(tagged).page(where = (Tagged::tag / Tag::topic) eq "odd").map(Tagged::id) }, equalTo(ids("t1", "t3")))
    }

    @Test
    fun `filters can exclude a value or match one of several`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.page(where = Note::author neq "a1").map(Note::id) }, equalTo(ids("n2", "n3", "n5", "n6", "n7")))
        assertThat(on(tenant) { it.page(where = Note::author oneOf listOf("a0", "a2")).map(Note::id) }, equalTo(ids("n2", "n3", "n5", "n6")))
        assertThat(on(tenant) { it.page(where = Note::author oneOf emptyList()).items }, equalTo(emptyList<Note>()))
    }

    @Test
    fun `count matches without loading rows`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.count() }, equalTo(7))
        assertThat(on(tenant) { it.count(Note::topic eq "odd") }, equalTo(4))
        assertThat(on(newTenant()) { it.count() }, equalTo(0))
    }

    @Test
    fun `pages can be ordered by a field, missing values last, with a limit but no cursor`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.page(order = descending(Note::size), limit = 3).map(Note::id) }, equalTo(ids("n7", "n6", "n5")))
        assertThat(on(tenant) { it.page(order = ascending(Note::author)).map(Note::id) }, equalTo(ids("n3", "n6", "n1", "n4", "n2", "n5", "n7")))
        assertThat(on(tenant) { it.first(order = descending(Note::author))?.id }, equalTo(RowId.of("n7")))
        assertThat(on(tenant) { it.page(order = ascending(Note::size), limit = 2).next }, absent())
        assertThat({ on(tenant) { it.page(order = ascending(Note::size), after = Cursor.of(1)) } }, throws<IllegalArgumentException>())
    }

    @Test
    fun `pages ordered by a time field are in time order, whatever its fraction`() {
        val tenant = newTenant()
        val second = Instant.parse("2026-11-21T10:00:00Z")
        on(tenant) { table ->
            table.insert(Note(RowId.of("later"), "x", null, 1, at = second.plusSeconds(1)))
            table.insert(Note(RowId.of("whole"), "x", null, 2, at = second))
            table.insert(Note(RowId.of("never"), "x", null, 3))
            table.insert(Note(RowId.of("half"), "x", null, 4, at = second.plusMillis(500)))
            table.insert(Note(RowId.of("micros"), "x", null, 5, at = second.plusNanos(1_000)))
        }

        assertThat(on(tenant) { it.page(order = ascending(Note::at)).map(Note::id) }, equalTo(ids("whole", "micros", "half", "later", "never")))
        assertThat(on(tenant) { it.page(order = descending(Note::at)).map(Note::id) }, equalTo(ids("never", "later", "half", "micros", "whole")))
    }

    @Test
    fun `pages ordered by a value type wrapping a time are in time order`() {
        val tenant = newTenant()
        val second = Instant.parse("2026-11-21T10:00:00Z")
        storage.write { tenants ->
            val table = tenants.tables(tenant)<RowId, Stamped>(stamped)
            table.insert(Stamped(RowId.of("half"), Stamp.of(second.plusMillis(500)), Moment.of(second.plusMillis(500))))
            table.insert(Stamped(RowId.of("whole"), Stamp.of(second), Moment.of(second)))
        }

        assertThat(storage.read { it.tables(tenant)<RowId, Stamped>(stamped).page(order = ascending(Stamped::stamp)).map(Stamped::id) }, equalTo(ids("whole", "half")))
        assertThat(storage.read { it.tables(tenant)<RowId, Stamped>(stamped).page(order = ascending(Stamped::moment)).map(Stamped::id) }, equalTo(ids("whole", "half")))
    }

    @Test
    fun `text sorts and compares by code point, so upper case comes before lower`() {
        val tenant = newTenant()
        on(tenant) { table -> listOf("apple", "Zebra", "banana").forEachIndexed { i, author -> table.insert(Note(RowId.of("n$i"), "t", author, i)) } }

        assertThat(on(tenant) { it.page(order = ascending(Note::author)).map { note -> note.author } }, equalTo(listOf<String?>("Zebra", "apple", "banana")))
        assertThat(on(tenant) { it.page(where = Note::author lt "a").map { note -> note.author } }, equalTo(listOf<String?>("Zebra")))
    }

    @Test
    fun `filters can match a range of numbers`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.page(where = Note::size gt 4).map(Note::id) }, equalTo(ids("n5", "n6", "n7")))
        assertThat(on(tenant) { it.page(where = Note::size lt 3).map(Note::id) }, equalTo(ids("n1", "n2")))
        assertThat(on(tenant) { it.page(where = Note::size.between(2, 5)).map(Note::id) }, equalTo(ids("n2", "n3", "n4")))
    }

    @Test
    fun `filters can match a range of times, whatever their fraction, and never match a missing one`() {
        val tenant = newTenant()
        val second = Instant.parse("2026-11-21T10:00:00Z")
        on(tenant) { table ->
            table.insert(Note(RowId.of("whole"), "x", null, 1, at = second))
            table.insert(Note(RowId.of("micros"), "x", null, 2, at = second.plusNanos(1_000)))
            table.insert(Note(RowId.of("half"), "x", null, 3, at = second.plusMillis(500)))
            table.insert(Note(RowId.of("later"), "x", null, 4, at = second.plusSeconds(1)))
            table.insert(Note(RowId.of("never"), "x", null, 5))
        }

        assertThat(on(tenant) { it.page(where = Note::at gt second).map(Note::id) }, equalTo(ids("micros", "half", "later")))
        assertThat(on(tenant) { it.page(where = Note::at lt second.plusMillis(500)).map(Note::id) }, equalTo(ids("whole", "micros")))
        assertThat(on(tenant) { it.page(where = Note::at.between(second.plusNanos(1_000), second.plusSeconds(1))).map(Note::id) }, equalTo(ids("micros", "half")))
    }

    @Test
    fun `filters can match a range of value types wrapping a time`() {
        val tenant = newTenant()
        val second = Instant.parse("2026-11-21T10:00:00Z")
        storage.write { tenants ->
            val table = tenants.tables(tenant)<RowId, Stamped>(stamped)
            table.insert(Stamped(RowId.of("whole"), Stamp.of(second), Moment.of(second)))
            table.insert(Stamped(RowId.of("half"), Stamp.of(second.plusMillis(500)), Moment.of(second.plusMillis(500))))
        }

        assertThat(storage.read { it.tables(tenant)<RowId, Stamped>(stamped).page(where = Stamped::stamp gt Stamp.of(second)).map(Stamped::id) }, equalTo(ids("half")))
        assertThat(storage.read { it.tables(tenant)<RowId, Stamped>(stamped).page(where = Stamped::moment lt Moment.of(second.plusMillis(500))).map(Stamped::id) }, equalTo(ids("whole")))
    }

    @Test
    fun `filters can match a range of dates`() {
        val tenant = newTenant()
        val day = LocalDate.parse("2026-11-21")
        on(tenant) { table -> (0L..3L).forEach { table.insert(Note(RowId.of("d$it"), "x", null, it.toInt(), day = day.plusDays(it))) } }

        assertThat(on(tenant) { it.page(where = Note::day.between(day.plusDays(1), day.plusDays(3))).map(Note::id) }, equalTo(ids("d1", "d2")))
    }

    @Test
    fun `filters can match either of two conditions, nested inside and`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.page(where = (Note::topic eq "even") or (Note::author eq null)).map(Note::id) }, equalTo(ids("n2", "n4", "n6", "n7")))
        assertThat(on(tenant) { it.page(where = (Note::topic eq "odd") and ((Note::author eq "a1") or (Note::author eq null))).map(Note::id) }, equalTo(ids("n1", "n7")))
        assertThat(on(tenant) { it.count((Note::size lt 2) or (Note::size gt 6)) }, equalTo(2))
    }

    @Test
    fun `dates and local times are in time order`() {
        val tenant = newTenant()
        val minute = LocalDateTime.parse("2026-11-21T10:00")
        on(tenant) { table ->
            table.insert(Note(RowId.of("half"), "x", null, 1, day = minute.toLocalDate().plusDays(1), local = minute.plusNanos(500_000_000)))
            table.insert(Note(RowId.of("whole"), "x", null, 2, day = minute.toLocalDate(), local = minute))
            table.insert(Note(RowId.of("later"), "x", null, 3, day = minute.toLocalDate().plusDays(2), local = minute.plusSeconds(30)))
        }

        assertThat(on(tenant) { it.page(order = ascending(Note::local)).map(Note::id) }, equalTo(ids("whole", "half", "later")))
        assertThat(on(tenant) { it.page(order = descending(Note::day)).map(Note::id) }, equalTo(ids("later", "half", "whole")))
    }

    @Test
    fun `a read that tries to write fails, and writes nothing`() {
        val tenant = newTenant()

        assertThat({ storage.read { it.tables(tenant)<RowId, Note>(notes).insert(Note(RowId.of("n1"), "x", null, 1)) } }, throws<Exception>())
        assertThat(on(tenant) { it.count() }, equalTo(0))
    }

    @Test
    fun `cursors page newest first and stay stable across inserts and deletes`() {
        val tenant = seeded()
        val first = on(tenant) { it.page(order = Order.Newest, limit = 3) }
        assertThat(first.map(Note::id), equalTo(ids("n7", "n6", "n5")))

        on(tenant) {
            it.insert(Note(RowId.of("n8"), "even", "a2", 8))
            it.delete(RowId.of("n4"))
        }

        val second = on(tenant) { it.page(order = Order.Newest, after = first.next, limit = 3) }
        assertThat(second.map(Note::id), equalTo(ids("n3", "n2", "n1")))
        assertThat(second.next, absent())
    }

    @Test
    fun `a row inserted after the newest rows are deleted comes after a cursor taken before`() {
        val tenant = seeded()
        val first = on(tenant) { it.page(limit = 5) }

        on(tenant) { table -> ids("n5", "n6", "n7").forEach(table::delete) }
        on(tenant) { it.insert(Note(RowId.of("n8"), "even", "a2", 8)) }

        assertThat(on(tenant) { it.page(after = first.next).map(Note::id) }, equalTo(ids("n8")))
    }

    @Test
    fun `no limit returns everything and no cursor, and tenants never mix`() {
        val tenant = seeded()
        val everything = on(tenant) { it.page() }
        assertThat(everything.map(Note::id), equalTo((1..7).map { RowId.of("n$it") }))
        assertThat(everything.next, absent())
        assertThat(on(newTenant()) { it.page().items }, equalTo(emptyList<Note>()))
    }

    @Test
    fun `a write belongs to one tenant, though a read can see several`() {
        val one = newTenant()
        val two = newTenant()

        assertThat({
            storage.write { tenants ->
                tenants.tables(one)<RowId, Note>(notes).insert(Note(RowId.of("a"), "x", null, 1))
                tenants.tables(two)<RowId, Note>(notes).insert(Note(RowId.of("b"), "x", null, 1))
            }
        }, throws<IllegalStateException>())

        assertThat(storage.read { tenants -> listOf(one, two).map { tenants.tables(it)<RowId, Note>(notes).count() } }, equalTo(listOf(0, 0)))
    }

    @Test
    fun `having finds only the tenants with matching rows`() {
        val tenant = seeded()
        val other = newTenant()
        on(other) { it.insert(Note(RowId.of("x"), "rare", null, 1)) }
        val withRare = storage.write { it.having(notes, Note::topic eq "rare") }
        assertThat(withRare, hasElement(other))
        assertThat(withRare, !hasElement(tenant))
    }

    @Test
    fun `contains matches part of a string field, whatever the ascii case, and combines`() {
        val tenant = seeded()
        on(tenant) { table ->
            table.insert(Note(RowId.of("l1"), "Lunch at noon", "zed", 8))
            table.insert(Note(RowId.of("l2"), "who wants LUNCH", "yak", 9))
            table.insert(Note(RowId.of("l3"), "something else", "zed", 10))
            table.insert(Note(RowId.of("l4"), "lunch again", null, 11))
        }

        assertThat(on(tenant) { it.page(where = Note::topic contains "lunch").map(Note::id) }, equalTo(ids("l1", "l2", "l4")))
        assertThat(on(tenant) { it.page(where = (Note::topic contains "LUNCH") and (Note::author eq "zed")).map(Note::id) }, equalTo(ids("l1")))
        assertThat(on(tenant) { it.page(where = (Note::topic contains "lun") and (Note::author eq null)).map(Note::id) }, equalTo(ids("l4")))
        assertThat(on(tenant) { it.count(Note::author contains "ZE") }, equalTo(2))
    }

    @Test
    fun `contains doesn't match a missing field`() {
        val tenant = seeded()
        assertThat(on(tenant) { it.page(where = Note::author contains "a").map(Note::id) }, equalTo(ids("n1", "n2", "n3", "n4", "n5", "n6")))
    }

    @Test
    fun `contains treats wildcards as plain text`() {
        val tenant = seeded()
        on(tenant) { table ->
            table.insert(Note(RowId.of("w1"), "100% done", null, 8))
            table.insert(Note(RowId.of("w2"), "100 done", null, 9))
            table.insert(Note(RowId.of("w3"), "snake_case", null, 10))
            table.insert(Note(RowId.of("w4"), "snakeXcase", null, 11))
            table.insert(Note(RowId.of("w5"), "back\\slash", null, 12))
        }

        assertThat(on(tenant) { it.page(where = Note::topic contains "0%").map(Note::id) }, equalTo(ids("w1")))
        assertThat(on(tenant) { it.page(where = Note::topic contains "e_c").map(Note::id) }, equalTo(ids("w3")))
        assertThat(on(tenant) { it.page(where = Note::topic contains "k\\s").map(Note::id) }, equalTo(ids("w5")))
    }

    @Test
    fun `contains finds text outside ascii when the case is the same`() {
        val tenant = seeded()
        on(tenant) { it.insert(Note(RowId.of("c1"), "un café", null, 8)) }

        assertThat(on(tenant) { it.page(where = Note::topic contains "café").map(Note::id) }, equalTo(ids("c1")))
    }

    @Test
    fun `having finds the tenants whose rows contain the text`() {
        val tenant = seeded()
        val other = seeded()
        on(tenant) { it.insert(Note(RowId.of("r1"), "a rare find", null, 8)) }

        val withRare = storage.read { it.having(notes, Note::topic contains "RARE FIND") }
        assertThat(withRare, hasElement(tenant))
        assertThat(withRare, !hasElement(other))
    }

    @Test
    fun `filters match numbers, booleans and composite values`() {
        val tenant = seeded()
        on(tenant) { it.insert(Note(RowId.of("p1"), "odd", null, 9, pinned = true)) }
        storage.write { tenants ->
            val table = tenants.tables(tenant)<RowId, Tagged>(tagged)
            (1..4).forEach { n -> table.insert(Tagged(RowId.of("t$n"), Tag(if (n % 2 == 0) "even" else "odd", n), n)) }
        }

        assertThat(on(tenant) { it.page(where = Note::size eq 3).map(Note::id) }, equalTo(ids("n3")))
        assertThat(on(tenant) { it.page(where = Note::size oneOf listOf(2, 9)).map(Note::id) }, equalTo(ids("n2", "p1")))
        assertThat(on(tenant) { it.page(where = Note::pinned eq true).map(Note::id) }, equalTo(ids("p1")))
        assertThat(on(tenant) { it.count(Note::pinned neq true) }, equalTo(7))
        assertThat(storage.read { it.tables(tenant)<RowId, Tagged>(tagged).page(where = Tagged::tag eq Tag("odd", 3)).map(Tagged::id) }, equalTo(ids("t3")))
    }
}
