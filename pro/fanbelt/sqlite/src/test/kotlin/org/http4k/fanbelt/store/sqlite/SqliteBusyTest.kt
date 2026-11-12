/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import org.http4k.fanbelt.store.Note
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.provisioned
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteConfig.JournalMode.WAL
import org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class SqliteBusyTest {
    private val file = File(Files.createTempDirectory("busy").toFile(), "busy.db").also { provisioned(SqliteDatabase(it), SqliteDocuments) }
    private val holder = SqliteDocumentStorage(file, Moshi)
    private val impatient = SQLiteConfig().apply {
        setJournalMode(WAL)
        transactionMode = IMMEDIATE
        setBusyTimeout(100)
    }
    private val storage = ExposedDocumentStorage(SqliteDatabase(file, impatient), Moshi, SqliteDocuments, readDb = SqliteDatabase(file, readSQLiteConfig))
    private val tenant = TenantId.of("busy")

    private fun writeWhileHeld(id: String) {
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        val held = CompletableFuture.runAsync {
            holder.write { tenants ->
                tenants.tables(tenant)<String, Note>(notes).insert(Note("held-$id", "topic", null, 1))
                holding.countDown()
                release.await()
            }
        }
        holding.await()

        val write = CompletableFuture.runAsync { storage.write { it.tables(tenant)<String, Note>(notes).insert(Note(id, "topic", null, 2)) } }
        Thread.sleep(250)
        release.countDown()
        held.get(5, SECONDS)
        write.get(5, SECONDS)
    }

    @Test
    fun `writes that find the database held past their busy timeout, more times than there are connections, are retried and all go through`() {
        (1..6).forEach { writeWhileHeld("n$it") }

        assertEquals(12, storage.read { it.tables(tenant)<String, Note>(notes).count() })
    }
}
