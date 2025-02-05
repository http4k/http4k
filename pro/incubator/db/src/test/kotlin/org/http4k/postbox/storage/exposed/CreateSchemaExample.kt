package org.http4k.postbox.storage.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.postbox.storage.exposed.ExposedPostboxSchema

fun main() {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    }).also { ExposedPostboxSchema.create(it) }
}

