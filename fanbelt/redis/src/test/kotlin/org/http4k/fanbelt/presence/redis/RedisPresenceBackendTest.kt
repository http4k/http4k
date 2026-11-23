/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.redis

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.lettuce.core.RedisClient
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.ConnectionId
import org.http4k.fanbelt.presence.PresenceBackendContract
import org.http4k.fanbelt.presence.PresenceConnection
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.redisUrl
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class RedisPresenceBackendTest : PresenceBackendContract() {

    override fun backend(): RedisPresenceBackend {
        return RedisPresenceBackend(Uri.of(redisUrl), clock)
    }

    @Test
    fun `closing a backend leaves a client it was given open for others`() {
        val client = RedisClient.create(redisUrl)
        try {
            RedisPresenceBackend(client, clock).apply { start() }.close()

            assertThat(client.connect().use { it.sync().ping() }, equalTo("PONG"))
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun `a stalled Redis fails presence calls within a second, so a redraw holding a permit gives it back`() {
        val topic = Topic.of("stalled-${UUID.randomUUID()}")
        val connection = PresenceConnection(topic, ConnectionId.of(UUID.randomUUID()), PresenceMember.of("alex"))
        val stalled = backend().apply { start() }
        val pauser = RedisClient.create(redisUrl)
        try {
            val paused = System.nanoTime()
            pauser.connect().use { it.sync().clientPause(PAUSE.toMillis()) }

            val took = listOf<() -> Unit>({ stalled.present(setOf(topic)) }, { stalled.join(listOf(connection), Duration.ofSeconds(30)) }, { stalled.set(topic, connection.member, "x") })
                .map { call -> System.nanoTime().also { runCatching(call) }.let { Duration.ofNanos(System.nanoTime() - it) } }

            assertThat(took.filter { it >= Duration.ofMillis(1500) }, equalTo(emptyList()))
            Thread.sleep(maxOf(0, PAUSE.toMillis() - Duration.ofNanos(System.nanoTime() - paused).toMillis()))
        } finally {
            pauser.shutdown()
            stalled.close()
        }
    }
}

private val PAUSE = Duration.ofSeconds(3)
