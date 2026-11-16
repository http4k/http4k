/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock.postgres

import org.http4k.fanbelt.lock.Locks
import java.sql.Connection

/**
 * Locks shared by every node on one Postgres, as session advisory locks: each held lock keeps a connection of its own,
 * and a node that dies drops its connection and so its locks.
 */
class PostgresLocks(private val connect: () -> Connection) : Locks {
    override fun tryLock(name: String): AutoCloseable? {
        val connection = connect()
        return runCatching {
            if (connection.advisory("pg_try_advisory_lock", name)) AutoCloseable {
                connection.use { it.advisory("pg_advisory_unlock", name) }
            } else null.also { connection.close() }
        }.onFailure { connection.close() }.getOrThrow()
    }
}

private fun Connection.advisory(function: String, name: String) =
    prepareStatement("select $function(hashtextextended(?, 0))").use { statement ->
        statement.setString(1, name)
        statement.executeQuery().use { it.next() && it.getBoolean(1) }
    }
