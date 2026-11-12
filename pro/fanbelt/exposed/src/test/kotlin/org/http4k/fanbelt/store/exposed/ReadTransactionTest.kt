/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.Note
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.notes
import org.http4k.fanbelt.store.sql
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import java.sql.DriverManager
import java.sql.SQLException

class ReadTransactionTest {
    private val tenant = TenantId.of("reads-${System.nanoTime()}")
    private val storage by lazy { ExposedDocumentStorage(provisioned(Database.connect(postgresUrl)), Moshi, PostgresDocuments) }

    @Test
    fun `a reader no longer aborts a contended writer`() {
        assertEquals(true, writerCommitsAround { storage.read { tenants -> tenants.tables(tenant).sql { it.connection.query("select v from anomaly_a", "select v from anomaly_b") } } })
    }

    @Test
    fun `a serializable reader would have aborted it`() {
        assertEquals(false, writerCommitsAround {
            serializable().use {
                it.query("select v from anomaly_a", "select v from anomaly_b")
                it.commit()
            }
        })
    }

    @Test
    fun `a state that tries to write fails`() {
        assertThrows<SQLException> {
            storage.read { it.tables(tenant)<String, Note>(notes).insert(Note("n1", "topic", null, 1)) }
        }
    }

    private fun writerCommitsAround(reader: () -> Unit): Boolean {
        provision()
        return serializable().use { slow ->
            slow.query("select v from anomaly_a", "select v from anomaly_b")
            serializable().use { quick ->
                quick.update("update anomaly_b set v = v + 1")
                quick.commit()
            }
            reader()
            runCatching {
                slow.update("update anomaly_a set v = v - 1")
                slow.commit()
            }.isSuccess
        }
    }

    private fun provision() = DriverManager.getConnection(postgresUrl).use {
        it.update(
            "drop table if exists anomaly_a",
            "drop table if exists anomaly_b",
            "create table anomaly_a (v int not null)",
            "create table anomaly_b (v int not null)",
            "insert into anomaly_a values (0)",
            "insert into anomaly_b values (0)",
        )
    }

    private fun serializable() = DriverManager.getConnection(postgresUrl).apply {
        autoCommit = false
        transactionIsolation = TRANSACTION_SERIALIZABLE
    }

    private fun Connection.query(vararg sql: String) = sql.forEach { statement -> createStatement().use { it.executeQuery(statement).close() } }

    private fun Connection.update(vararg sql: String) = sql.forEach { statement -> createStatement().use { it.execute(statement) } }
}
