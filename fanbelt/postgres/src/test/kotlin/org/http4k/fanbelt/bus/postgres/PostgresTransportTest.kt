package org.http4k.fanbelt.bus.postgres

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.TransportContract
import org.http4k.fanbelt.eventually
import org.http4k.fanbelt.store.postgres.postgresUrl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class PostgresTransportTest : TransportContract() {
    private val scheduler = Executors.newScheduledThreadPool(4)

    @AfterEach
    fun stop() {
        scheduler.shutdownNow()
    }

    override fun transport() =
        PostgresTransport({ DriverManager.getConnection("$postgresUrl&ApplicationName=transport") }, scheduler, {}, retryDelay = Duration.ofMillis(50))

    @Test
    fun `topics sent together share one connection`() {
        val connects = AtomicInteger()
        val transport = PostgresTransport({ connects.incrementAndGet(); DriverManager.getConnection(postgresUrl) }, scheduler, {})

        transport.send((1..5).map { Topic.of("acme/t$it") }.toSet())

        assertThat(connects.get(), equalTo(1))
    }

    @Test
    fun `connections that don't autocommit still deliver`() {
        val heard = CopyOnWriteArrayList<Topic>()
        val connect = { DriverManager.getConnection(postgresUrl).apply { autoCommit = false } }
        val listener = PostgresTransport(connect, scheduler, {}).apply { start({ heard += it }, {}) }
        val sent = Topic.of("acme/${UUID.randomUUID()}")

        try {
            eventually {
                PostgresTransport(connect, scheduler, {}).send(setOf(sent))
                Thread.sleep(20)
                sent in heard
            }
        } finally {
            listener.close()
        }
    }

    override fun dropConnections() {
        DriverManager.getConnection(postgresUrl).use { connection ->
            connection.createStatement().use { it.execute("select pg_terminate_backend(pid) from pg_stat_activity where application_name = 'transport'") }
        }
    }
}
