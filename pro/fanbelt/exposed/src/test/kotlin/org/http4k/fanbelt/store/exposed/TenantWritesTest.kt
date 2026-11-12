/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.Note
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

class TenantWritesTest {
    private val storage by lazy { ExposedDocumentStorage(provisioned(Database.connect(postgresUrl)), Moshi, PostgresDocuments) }
    private val acme = TenantId.of("acme-${System.nanoTime()}")
    private val globex = TenantId.of("globex-${System.nanoTime()}")

    @Test
    fun `concurrent writes to one tenant queue rather than retry, and none is lost`() {
        storage.write { it.tables(acme)<String, Note>(notes).insert(Note("counter", "count", null, 0)) }
        val writers = Executors.newFixedThreadPool(16)

        val attempts = AtomicInteger()

        val outcomes = (1..50).map {
            writers.submit<Result<Unit>> {
                runCatching {
                    storage.write {
                        attempts.incrementAndGet()
                        it.tables(acme)<String, Note>(notes).update("counter") { note -> note.copy(size = note.size + 1) }
                    }
                }
            }
        }.map { it.get(60, SECONDS) }
        writers.shutdown()

        assertEquals(0, outcomes.count { it.isFailure })
        assertEquals(50, attempts.get())
        assertEquals(50, storage.read { it.tables(acme)<String, Note>(notes).get("counter") }?.size)
    }

    @Test
    fun `writes in different tenants never make each other retry`() {
        val bothRead = CyclicBarrier(2)
        val writers = Executors.newFixedThreadPool(2)

        val attempts = listOf(acme, globex).map { tenant ->
            writers.submit<Int> {
                var attempt = 0
                storage.write { tenants ->
                    val table = tenants.tables(tenant)<String, Note>(notes)
                    table.page(where = Note::topic eq "scan")
                    if (attempt++ == 0) bothRead.await(10, SECONDS)
                    table.insert(Note("n", "scan", null, 1))
                }
                attempt
            }
        }.map { it.get(60, SECONDS) }
        writers.shutdown()

        assertEquals(listOf(1, 1), attempts)
        assertEquals(listOf(1, 1), listOf(acme, globex).map { tenant -> storage.read { it.tables(tenant)<String, Note>(notes).page().items.size } })
    }
}
