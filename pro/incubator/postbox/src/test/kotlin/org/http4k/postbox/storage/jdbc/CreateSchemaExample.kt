/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.opentest4j.TestAbortedException

fun main() {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    }).also { JdbcPostboxSchema.create(it) }
}


fun postgresDataSource(prefix: String = "http4k") = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    }).also { JdbcPostboxSchema.create(it, prefix) }
} catch (e: Exception) {
    System.err.println("Postgres not available")
    e.printStackTrace(System.err)
    throw TestAbortedException("Postgres not available", e)
}

