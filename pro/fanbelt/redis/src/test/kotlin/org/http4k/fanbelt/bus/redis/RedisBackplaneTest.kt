/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.redis

import io.lettuce.core.RedisClient
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.Backplane
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.redisUrl
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class RedisBackplaneTest {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val nodes = mutableListOf<AutoCloseable>()

    private fun node() = Backplane(RedisTransport(Uri.of(redisUrl), {}), scheduler, {}, Duration.ofMinutes(5)).apply {
        start()
        nodes += this
    }

    @AfterEach
    fun close() = nodes.forEach(AutoCloseable::close)

    @Test
    fun `a node hears a tenant's changes only while it has watchers there`() {
        val sender = node()
        val listener = node()
        val acme = TenantId.of("acme-${System.nanoTime()}")(Topic.of("t"))
        val beta = TenantId.of("beta-${System.nanoTime()}")(Topic.of("t"))
        val gamma = TenantId.of("gamma-${System.nanoTime()}")(Topic.of("t"))
        val heard = CopyOnWriteArrayList<Topic>()
        val watching = listener.subscribe(setOf(acme)) { heard += acme }
        listener.subscribe(setOf(beta)) {}.close()

        sender.publish(beta)
        sender.publish(acme)
        eventually { assertEquals(listOf(acme), heard) }
        watching.close()
        listener.subscribe(setOf(gamma)) { heard += gamma }
        sender.publish(acme)
        sender.publish(gamma)

        eventually { assertEquals(listOf(acme, gamma), heard) }
    }

    @Test
    fun `closing a backplane leaves a client it was given open for others`() {
        val client = RedisClient.create(redisUrl)
        try {
            Backplane(RedisTransport(client, {}), scheduler, {}, Duration.ofMinutes(5)).apply { start() }.close()

            assertEquals("PONG", client.connect().use { it.sync().ping() })
        } finally {
            client.shutdown()
        }
    }
}
