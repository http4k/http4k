package org.http4k.postbox.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.opentest4j.TestAbortedException

fun postgresDataSource(prefix: String = "http4k") = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    }).also { ExposedPostboxSchema.create(it, prefix) }
} catch (e: Exception) {
    throw TestAbortedException("Postgres not available", e)
}
