/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.tx.linearBackoff
import dev.forkhandles.tx.maxAttempts
import org.http4k.fanbelt.store.Note
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.provisioned
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.junit.jupiter.api.Test
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteConfig.JournalMode.WAL
import org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.SECONDS

class SqliteConcurrencyTest {
    private val file = File(Files.createTempDirectory("concurrency").toFile(), "concurrency.db").also { provisioned(SqliteDatabase(it), SqliteDocuments) }
    private val storage = SqliteDocumentStorage(file, Moshi)
    private val impatient = SQLiteConfig().apply {
        setJournalMode(WAL)
        transactionMode = IMMEDIATE
        setBusyTimeout(10)
    }
    private val retries = Semaphore(0)
    private val impatientStorage = ExposedDocumentStorage(
        SqliteDatabase(file, impatient), Moshi, SqliteDocuments,
        retryPolicy = { attempt -> retries.release(); linearBackoff(Duration.ofMillis(5)).maxAttempts(5)(attempt) },
        readDb = SqliteDatabase(file, readSQLiteConfig))
    private val tenant = TenantId.of("concurrency")

    private fun writeWhileHeld(id: String) {
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        val held = CompletableFuture.runAsync {
            storage.write { tenants ->
                tenants.tables(tenant)<String, Note>(notes).insert(Note("held-$id", "topic", null, 1))
                holding.countDown()
                release.await()
            }
        }
        holding.await()
        retries.drainPermits()

        val write = CompletableFuture.runAsync { impatientStorage.write { it.tables(tenant)<String, Note>(notes).insert(Note(id, "topic", null, 2)) } }
        retries.acquire()
        release.countDown()
        held.get(5, SECONDS)
        write.get(5, SECONDS)
    }

    @Test
    fun `a read does not wait for a write that is still open`() {
        val writing = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val writer = CompletableFuture.runAsync {
            storage.write { tenants ->
                tenants.tables(tenant)<String, Note>(notes).insert(Note("n1", "topic", null, 1))
                writing.countDown()
                finish.await()
            }
        }
        writing.await()

        val read = CompletableFuture.supplyAsync { storage.read { it.tables(tenant)<String, Note>(notes).count() } }

        try {
            assertThat(read.get(1, SECONDS), equalTo(0))
        } finally {
            finish.countDown()
            writer.get(5, SECONDS)
        }
    }

    @Test
    fun `writes that find the database held past their busy timeout, more times than there are connections, are retried and all go through`() {
        (1..6).forEach { writeWhileHeld("n$it") }

        assertThat(impatientStorage.read { it.tables(tenant)<String, Note>(notes).count() }, equalTo(12))
    }
}
