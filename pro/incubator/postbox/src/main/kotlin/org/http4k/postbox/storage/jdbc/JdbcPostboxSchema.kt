/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.jdbc

import javax.sql.DataSource

object JdbcPostboxSchema {

    fun create(dataSource: DataSource, prefix: String = "http4k") {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(createTableSql(prefix))
            }
        }
    }

    private fun createTableSql(prefix: String) = """
        CREATE TABLE IF NOT EXISTS ${prefix}_postbox (
            request_id  VARCHAR(36)  NOT NULL,
            request     TEXT         NOT NULL,
            response    TEXT,
            created_at  TIMESTAMP    NOT NULL,
            process_at  TIMESTAMP    NOT NULL,
            failures    INT          NOT NULL DEFAULT 0,
            status      VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
            CONSTRAINT ${prefix}_request_id_pk PRIMARY KEY (request_id)
        )
    """.trimIndent()
}
