package org.http4k.postbox.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.db.ExposedTransactor
import org.http4k.postbox.PostboxContract
import org.opentest4j.TestAbortedException

class ExposedPostboxTest : PostboxContract() {
    override val postbox = ExposedTransactor(testDataSource(), { ExposedPostbox() })
}

private fun testDataSource() = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    }).also { ExposedPostboxSchema.create(it) }
} catch (e: Exception) {
    throw TestAbortedException("Postgres not available", e)
}

