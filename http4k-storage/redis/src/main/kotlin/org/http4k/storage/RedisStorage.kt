package org.http4k.storage

import io.lettuce.core.RedisClient.create
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs.Builder
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import org.http4k.core.Uri
import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi
import java.net.URI
import java.time.Duration
import java.time.Duration.ofHours

/**
 * Connect to Redis using Automarshalling
 */
inline fun <reified T : Any> Storage.Companion.Redis(uri: Uri, autoMarshalling: AutoMarshalling = Moshi) =
    Redis(uri, AutoRedisCodec<T>(autoMarshalling))

/**
 * Connect to Redis using custom codec
 */
fun <T : Any> Storage.Companion.Redis(uri: Uri, codec: RedisCodec<String, T>) =
    Redis(create(uri.asRedis()).connect(codec).sync())


/**
 * Redis-backed storage implementation. You probably want to use one of the builder functions instead of this
 */
fun <T : Any> Storage.Companion.Redis(redis: RedisCommands<String, T>, ttl: Duration = ofHours(1)) = object :
    Storage<T> {

    override fun get(key: String): T? = redis.get(key)

    override fun set(key: String, data: T) {
        val result = redis.set(key, data, Builder.ex(ttl.seconds))
        if (result != "OK") throw RuntimeException(result)
    }

    override fun remove(key: String): Boolean = redis.del(key) >= 1L

    override fun removeAll(keyPrefix: String): Boolean {
        val keys = redis.keys("$keyPrefix*")
        return if (keys.isEmpty()) false
        else {
            redis.del(*keys.toTypedArray())
            true
        }
    }

    override fun keySet(keyPrefix: String) =
        redis.keys("$keyPrefix*").toSet()
}

fun Uri.asRedis() = RedisURI.create(URI(toString()))
