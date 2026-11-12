/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import org.http4k.fanbelt.store.Note
import org.http4k.fanbelt.store.exposed.provisioned
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class SqliteReadsTest {
    private val file = File(Files.createTempDirectory("reads").toFile(), "reads.db").also { provisioned(SqliteDatabase(it), SqliteDocuments) }
    private val storage = SqliteDocumentStorage(file, Moshi)
    private val tenant = TenantId.of("reads")

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
            assertEquals(0, read.get(1, SECONDS))
        } finally {
            finish.countDown()
            writer.get(5, SECONDS)
        }
    }
}
