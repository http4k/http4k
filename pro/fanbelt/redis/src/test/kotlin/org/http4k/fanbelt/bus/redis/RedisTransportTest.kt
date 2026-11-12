/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.redis

import io.lettuce.core.KillArgs
import io.lettuce.core.RedisClient
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.TransportContract
import org.http4k.fanbelt.redisUrl

class RedisTransportTest : TransportContract() {
    override fun transport() = RedisTransport(Uri.of(redisUrl), {})

    override fun dropConnections() {
        val client = RedisClient.create(redisUrl)
        try {
            client.connect().use { it.sync().clientKill(KillArgs.Builder.typePubsub()) }
        } finally {
            client.shutdown()
        }
    }
}
