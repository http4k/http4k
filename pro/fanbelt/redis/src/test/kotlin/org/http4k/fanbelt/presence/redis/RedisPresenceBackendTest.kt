/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.redis

import io.lettuce.core.RedisClient
import org.http4k.core.Uri
import org.http4k.fanbelt.presence.PresenceBackendContract
import org.http4k.fanbelt.redisUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisPresenceBackendTest : PresenceBackendContract() {

    override fun backend(): RedisPresenceBackend {
        return RedisPresenceBackend(Uri.of(redisUrl), clock)
    }

    @Test
    fun `closing a backend leaves a client it was given open for others`() {
        val client = RedisClient.create(redisUrl)
        try {
            RedisPresenceBackend(client, clock).apply { start() }.close()

            assertEquals("PONG", client.connect().use { it.sync().ping() })
        } finally {
            client.shutdown()
        }
    }
}
