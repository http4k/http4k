package org.http4k.fanbelt.store.postgres

import org.testcontainers.postgresql.PostgreSQLContainer

val postgresUrl: String by lazy {
    val postgres = PostgreSQLContainer("postgres:17-alpine")
        .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
        .withEnv("POSTGRES_INITDB_ARGS", "--locale-provider=icu --icu-locale=en-US")
        .withUrlParam("user", "test")
    postgres.start()
    postgres.jdbcUrl
}
