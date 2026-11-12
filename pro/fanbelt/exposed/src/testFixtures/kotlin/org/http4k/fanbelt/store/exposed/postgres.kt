/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.testcontainers.postgresql.PostgreSQLContainer

val postgresUrl: String by lazy {
    val postgres = PostgreSQLContainer("postgres:17-alpine")
        .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
        .withUrlParam("user", "test")
    postgres.start()
    postgres.jdbcUrl
}
