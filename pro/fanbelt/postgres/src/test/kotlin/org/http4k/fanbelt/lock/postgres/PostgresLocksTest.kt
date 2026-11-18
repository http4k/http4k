/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock.postgres

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.lock.LocksContract
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.postgres.PostgresDocuments
import org.http4k.fanbelt.store.postgres.postgresUrl
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.CopyOnWriteArrayList

class PostgresLocksTest : LocksContract() {
    override val locks = ExposedDocumentStorage(Database.connect(postgresUrl), Moshi, PostgresDocuments).locks
    override val elsewhere = ExposedDocumentStorage(Database.connect(postgresUrl), Moshi, PostgresDocuments).locks

    private val opened = CopyOnWriteArrayList<Connection>()
    private val counted = PostgresLocks { DriverManager.getConnection(postgresUrl).also { opened += it } }
    private val prefix = "held/${System.nanoTime()}"

    @Test
    fun `a node holds all its locks on one connection, so held locks don't use up the pool`() {
        val held = (1..3).map { counted.tryLock("$prefix/$it")!! }
        held.forEach(AutoCloseable::close)

        assertThat(opened.size, equalTo(1))
    }

    @Test
    fun `a lock connection that drops is opened again, so locks can still be taken`() {
        counted.tryLock("$prefix/before")!!.close()
        terminate(opened.single())

        val after = counted.tryLock("$prefix/after")?.also { it.close() }

        assertThat((after != null) to opened.size, equalTo(true to 2))
    }

    private fun terminate(connection: Connection) {
        val pid = connection.createStatement().use { it.executeQuery("select pg_backend_pid()").use { rows -> rows.next(); rows.getInt(1) } }
        DriverManager.getConnection(postgresUrl).use { admin ->
            admin.prepareStatement("select pg_terminate_backend(?)").use { it.setInt(1, pid); it.executeQuery().close() }
        }
    }
}
