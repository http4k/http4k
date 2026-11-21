/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.postgres

import org.http4k.fanbelt.store.Locks
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

/**
 * Locks shared by every node on one Postgres, as session advisory locks all held on one connection of this node's own.
 */
class PostgresLocks(private val connect: () -> Connection) : Locks {
    private var session: Connection? = null

    private val held = ConcurrentHashMap.newKeySet<String>()

    override fun invoke(name: String, block: () -> Unit): Boolean {
        if (!held.add(name)) return false
        try {
            return acrossNodes(name, block)
        } finally {
            held.remove(name)
        }
    }

    private fun acrossNodes(name: String, block: () -> Unit): Boolean {
        if (!lock(name)) return false
        try {
            block()
        } finally {
            unlock(name)
        }
        return true
    }

    @Synchronized
    private fun lock(name: String) = advisory("pg_try_advisory_lock", name)

    @Synchronized
    private fun unlock(name: String) {
        runCatching { advisory("pg_advisory_unlock", name) }
    }

    private fun advisory(function: String, name: String) = try {
        session().advisory(function, name)
    } catch (e: SQLException) {
        session?.runCatching { close() }
        session = null
        session().advisory(function, name)
    }

    private fun session() = session ?: connect().also { session = it }
}

private fun Connection.advisory(function: String, name: String) =
    prepareStatement("select $function(hashtextextended(?, 0))").use { statement ->
        statement.setString(1, name)
        statement.executeQuery().use { it.next() && it.getBoolean(1) }
    }
