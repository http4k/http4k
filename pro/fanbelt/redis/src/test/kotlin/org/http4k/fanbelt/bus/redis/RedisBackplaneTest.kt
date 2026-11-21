/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.redis

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.lettuce.core.RedisClient
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.redisUrl
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class RedisBackplaneTest {
    private val nodes = mutableListOf<AutoCloseable>()

    private fun node() = AppInfra(AppRuntime(), transport = RedisTransport(Uri.of(redisUrl), {})).apply {
        start()
        nodes += this
    }

    @AfterEach
    fun close() = nodes.forEach(AutoCloseable::close)

    @Test
    fun `a node hears a tenant's changes only while it has watchers there`() {
        val sender = node()
        val listener = node()
        val acme = TenantId.of("acme-${System.nanoTime()}")
        val beta = TenantId.of("beta-${System.nanoTime()}")
        val gamma = TenantId.of("gamma-${System.nanoTime()}")
        val t = Topic.of("t")
        val heard = CopyOnWriteArrayList<TenantId>()
        val watching = listener.scope(acme).bus.subscribe(setOf(t)) { heard += acme }
        listener.scope(beta).bus.subscribe(setOf(t)) {}.close()

        sender.scope(beta).bus.publish(t)
        sender.scope(acme).bus.publish(t)
        eventually { assertThat(heard, equalTo(listOf(acme))) }
        watching.close()
        listener.scope(gamma).bus.subscribe(setOf(t)) { heard += gamma }
        sender.scope(acme).bus.publish(t)
        sender.scope(gamma).bus.publish(t)

        eventually { assertThat(heard, equalTo(listOf(acme, gamma))) }
    }

    @Test
    fun `closing a node leaves a client it was given open for others`() {
        val client = RedisClient.create(redisUrl)
        try {
            AppInfra(AppRuntime(), transport = RedisTransport(client, {})).apply { start() }.close()

            assertThat(client.connect().use { it.sync().ping() }, equalTo("PONG"))
        } finally {
            client.shutdown()
        }
    }
}
