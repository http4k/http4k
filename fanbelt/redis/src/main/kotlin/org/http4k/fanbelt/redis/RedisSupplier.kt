package org.http4k.fanbelt.redis

import io.lettuce.core.RedisClient
import org.http4k.core.Uri
import org.http4k.fanbelt.infrastructure.Lifecycle

internal sealed interface RedisSupplier : Lifecycle {
    val client: RedisClient

    class Owned(private val uri: Uri) : RedisSupplier {
        override lateinit var client: RedisClient

        override fun start() {
            client = RedisClient.create(uri.toString())
        }

        override fun close() = client.shutdown()
    }

    class Borrowed(override val client: RedisClient) : RedisSupplier
}
