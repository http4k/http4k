/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.postgres

import org.http4k.fanbelt.bus.TransportContract
import org.http4k.fanbelt.store.exposed.postgresUrl
import org.junit.jupiter.api.AfterEach
import java.sql.DriverManager
import java.time.Duration
import java.util.concurrent.Executors

class PostgresTransportTest : TransportContract() {
    private val scheduler = Executors.newScheduledThreadPool(4)

    @AfterEach
    fun stop() {
        scheduler.shutdownNow()
    }

    override fun transport() =
        PostgresTransport({ DriverManager.getConnection("$postgresUrl&ApplicationName=transport") }, scheduler, {}, retryDelay = Duration.ofMillis(50))

    override fun dropConnections() {
        DriverManager.getConnection(postgresUrl).use { connection ->
            connection.createStatement().use { it.execute("select pg_terminate_backend(pid) from pg_stat_activity where application_name = 'transport'") }
        }
    }
}
