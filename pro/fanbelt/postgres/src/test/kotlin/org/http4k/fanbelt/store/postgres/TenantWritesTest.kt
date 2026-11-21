/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.eventually
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.Note
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.exposed.provisioned
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

class TenantWritesTest {
    private val app = "writes-${System.nanoTime()}"
    private val db by lazy { provisioned(Database.connect("$postgresUrl&ApplicationName=$app"), PostgresDocumentStorage::schema) }
    private val storage by lazy { PostgresDocumentStorage(db, Moshi) }
    private val keyed by lazy { PostgresDocumentStorage(db, Moshi, KeyLocking) }
    private val random = LockKey.of("random")
    private val eng = LockKey.of("eng")
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

        assertThat(outcomes.count { it.isFailure }, equalTo(0))
        assertThat(attempts.get(), equalTo(50))
        assertThat(storage.read { it.tables(acme)<String, Note>(notes).get("counter") }?.size, equalTo(50))
    }

    @Test
    fun `with key locks, concurrent writes to one key wait and retry, and none is lost or fails`() {
        keyed.write { it.tables(acme)<String, Note>(notes).insert(Note("counter", "count", null, 0)) }
        val writers = Executors.newFixedThreadPool(16)

        val outcomes = (1..50).map {
            writers.submit<Result<Unit>> {
                runCatching {
                    keyed.write { it.tables(acme, random)<String, Note>(notes).update("counter") { note -> note.copy(size = note.size + 1) } }
                }
            }
        }.map { it.get(60, SECONDS) }
        writers.shutdown()

        assertThat(outcomes.count { it.isFailure }, equalTo(0))
        assertThat(keyed.read { it.tables(acme)<String, Note>(notes).get("counter") }?.size, equalTo(50))
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

        assertThat(attempts, equalTo(listOf(1, 1)))
        assertThat(listOf(acme, globex).map { tenant -> storage.read { it.tables(tenant)<String, Note>(notes).page().items.size } }, equalTo(listOf(1, 1)))
    }

    @Test
    fun `the tenant lock ignores keys`() {
        assertThat(storage.waits(holding = random, wanting = eng), equalTo(true))
    }

    @Test
    fun `with key locks, writes to different keys of one tenant don't wait for each other, but writes to one key or the whole tenant do`() {
        assertThat(keyed.waits(holding = random, wanting = eng), equalTo(false))
        assertThat(keyed.waits(holding = random, wanting = random), equalTo(true))
        assertThat(keyed.waits(holding = random, wanting = null), equalTo(true))
        assertThat(keyed.waits(holding = null, wanting = random), equalTo(true))
    }

    @Test
    fun `with key locks, writes with different keys that change the same row retry rather than lose an update`() {
        keyed.write { it.tables(acme)<String, Note>(notes).insert(Note("counter", "count", null, 0)) }
        val bothRead = CyclicBarrier(2)
        val writers = Executors.newFixedThreadPool(2)

        val attempts = listOf(random, eng).map { key ->
            writers.submit<Int> {
                var attempt = 0
                keyed.write { tenants ->
                    val table = tenants.tables(acme, key)<String, Note>(notes)
                    table.get("counter")
                    if (attempt++ == 0) bothRead.await(10, SECONDS)
                    table.update("counter") { note -> note.copy(size = note.size + 1) }
                }
                attempt
            }
        }.map { it.get(60, SECONDS) }
        writers.shutdown()

        assertThat(attempts.sum(), equalTo(3))
        assertThat(keyed.read { it.tables(acme)<String, Note>(notes).get("counter") }?.size, equalTo(2))
    }

    private fun DocumentStorage.waits(holding: LockKey?, wanting: LockKey?): Boolean {
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val got = CountDownLatch(1)
        val writers = Executors.newFixedThreadPool(2)

        val first = writers.submit {
            write {
                it.tables(acme, holding)
                held.countDown()
                release.await(10, SECONDS)
            }
        }
        held.await(10, SECONDS)
        val second = writers.submit { write { it.tables(acme, wanting); got.countDown() } }
        eventually { got.count == 0L || blocked() }
        val waited = got.count != 0L
        release.countDown()
        first.get(10, SECONDS)
        second.get(10, SECONDS)
        writers.shutdown()
        return waited
    }

    private fun blocked() = DriverManager.getConnection(postgresUrl).use { connection ->
        connection.createStatement().use {
            it.executeQuery("select count(*) from pg_stat_activity where application_name = '$app' and cardinality(pg_blocking_pids(pid)) > 0")
                .run { next(); getInt(1) > 0 }
        }
    }
}
