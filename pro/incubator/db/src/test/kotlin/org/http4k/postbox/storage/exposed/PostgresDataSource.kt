package org.http4k.postbox.storage.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.postbox.storage.exposed.ExposedPostboxSchema
import org.opentest4j.TestAbortedException

fun postgresDataSource(prefix: String = "http4k") = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    }).also { ExposedPostboxSchema.create(it, prefix) }
} catch (e: Exception) {
    System.err.println("Postgres not available")
    e.printStackTrace(System.err)
    throw TestAbortedException("Postgres not available", e)
}
