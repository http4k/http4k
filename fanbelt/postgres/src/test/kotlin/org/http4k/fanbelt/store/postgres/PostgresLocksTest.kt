package org.http4k.fanbelt.store.postgres

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.store.LocksContract
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.CopyOnWriteArrayList

class PostgresLocksTest : LocksContract() {
    override val locks = PostgresDocumentStorage(Database.connect(postgresUrl), Moshi).locks
    override val elsewhere = PostgresDocumentStorage(Database.connect(postgresUrl), Moshi).locks

    private val opened = CopyOnWriteArrayList<Connection>()
    private val counted = PostgresLocks { DriverManager.getConnection(postgresUrl).also { opened += it } }
    private val prefix = "held/${System.nanoTime()}"

    @Test
    fun `a node holds all its locks on one connection, so held locks don't use up the pool`() {
        counted("$prefix/1") { counted("$prefix/2") { counted("$prefix/3") {} } }

        assertThat(opened.size, equalTo(1))
    }

    @Test
    fun `a lock connection that drops is opened again, so locks can still be taken`() {
        counted("$prefix/before") {}
        terminate(opened.single())

        val after = counted("$prefix/after") {}

        assertThat(after to opened.size, equalTo(true to 2))
    }

    private fun terminate(connection: Connection) {
        val pid = connection.createStatement().use { it.executeQuery("select pg_backend_pid()").use { rows -> rows.next(); rows.getInt(1) } }
        DriverManager.getConnection(postgresUrl).use { admin ->
            admin.prepareStatement("select pg_terminate_backend(?)").use { it.setInt(1, pid); it.executeQuery().close() }
        }
    }
}
