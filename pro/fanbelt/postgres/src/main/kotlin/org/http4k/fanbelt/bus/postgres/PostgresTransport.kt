/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.postgres

import org.http4k.events.Events
import org.http4k.fanbelt.bus.BackplaneConnectFailed
import org.http4k.fanbelt.bus.BackplaneDisconnected
import org.http4k.fanbelt.bus.ChannelName
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.Transport
import org.postgresql.PGConnection
import java.sql.Connection
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS

class PostgresTransport(
    private val connect: () -> Connection,
    private val scheduler: ScheduledExecutorService,
    private val events: Events,
    private val channel: ChannelName = ChannelName.default,
    private val retryDelay: Duration = Duration.ofSeconds(1),
    private val pollTimeout: Duration = Duration.ofSeconds(1),
) : Transport {
    @Volatile
    private var running = false

    @Volatile
    private var listening: Connection? = null

    private lateinit var received: (Topic) -> Unit
    private lateinit var reconnected: () -> Unit

    override fun send(topic: Topic) {
        connect().use { connection ->
            connection.prepareStatement("SELECT pg_notify(?, ?)").use {
                it.setString(1, channel.value)
                it.setString(2, topic.value)
                it.execute()
            }
        }
    }

    override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {
        this.received = received
        this.reconnected = reconnected
        running = true
        scheduler.execute(::poll)
    }

    override fun close() {
        running = false
        listening?.close()
    }

    private fun poll() {
        if (!running) return
        val next = runCatching {
            val connection = listening ?: listen()
            val notifications = connection.unwrap(PGConnection::class.java).getNotifications(pollTimeout.toMillis().toInt())
            if (notifications.isNullOrEmpty()) {
                check(connection.isValid(5)) { "backplane connection lost" }
            } else {
                notifications.forEach { received(Topic.of(it.parameter)) }
            }
            Duration.ZERO
        }.getOrElse {
            events(if (listening == null) BackplaneConnectFailed(it) else BackplaneDisconnected(it))
            runCatching { listening?.close() }
            listening = null
            retryDelay
        }
        if (running) scheduler.schedule(::poll, next.toMillis(), MILLISECONDS)
    }

    private fun listen(): Connection {
        val connection = connect()
        connection.createStatement().use { it.execute("LISTEN $channel") }
        listening = connection
        reconnected()
        return connection
    }
}
