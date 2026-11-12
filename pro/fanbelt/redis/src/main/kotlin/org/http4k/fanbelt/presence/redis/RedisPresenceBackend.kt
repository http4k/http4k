/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.redis

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import io.lettuce.core.Range
import io.lettuce.core.RedisClient
import io.lettuce.core.ScoredValue
import io.lettuce.core.ZRange
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.ConnectionId
import org.http4k.fanbelt.presence.PresenceBackend
import org.http4k.fanbelt.presence.PresenceConnection
import org.http4k.fanbelt.presence.PresenceMember
import org.http4k.fanbelt.redis.RedisSupplier
import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi
import java.time.Clock
import java.time.Duration
import kotlin.reflect.KClass

class RedisPresenceBackend internal constructor(
    private val supplier: RedisSupplier,
    private val clock: Clock,
    private val json: AutoMarshalling,
    private val prefix: KeyPrefix,
) : PresenceBackend {
    constructor(uri: Uri, clock: Clock, json: AutoMarshalling = Moshi, prefix: KeyPrefix = KeyPrefix.default) :
        this(RedisSupplier.Owned(uri), clock, json, prefix)

    constructor(client: RedisClient, clock: Clock, json: AutoMarshalling = Moshi, prefix: KeyPrefix = KeyPrefix.default) :
        this(RedisSupplier.Borrowed(client), clock, json, prefix)

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var redis: RedisCommands<String, String>
    private lateinit var pipelined: RedisAsyncCommands<String, String>

    override fun start() {
        supplier.start()
        connection = supplier.client.connect()
        redis = connection.sync()
        pipelined = connection.async()
    }

    override fun join(connections: List<PresenceConnection>, ttl: Duration) {
        if (connections.isEmpty()) return
        val expiresAt = (clock.millis() + ttl.toMillis()).toDouble()
        val renewed = connections.groupBy { it.topic }.map { (topic, joined) ->
            pipelined.zadd(zset(topic), *joined.map { ScoredValue.just(expiresAt, PresenceEntry.of(it.id, it.member).value) }.toTypedArray())
        }
        val indexed = pipelined.sadd(index, *connections.map { it.topic.value }.distinct().toTypedArray())
        (renewed + indexed).forEach { it.get() }
    }

    override fun leave(connection: PresenceConnection) {
        redis.zrem(zset(connection.topic), PresenceEntry.of(connection.id, connection.member).value)
        forget(connection.topic)
    }

    override fun set(topic: Topic, member: PresenceMember, value: Any) {
        if (member in live(topic)) redis.hset(values(topic), member.value, json.asFormatString(value))
    }

    override fun <V : Any> present(topic: Topic, type: KClass<V>) = asking(topic, type)()

    override fun present(topics: Map<Topic, KClass<out Any>>): Map<Topic, Map<PresenceMember, Any?>> =
        topics.mapValues { (topic, type) -> asking(topic, type) }.mapValues { (_, answer) -> answer() }

    private fun <V : Any> asking(topic: Topic, type: KClass<V>): () -> Map<PresenceMember, V?> {
        val carried = pipelined.hgetall(values(topic))
        val here = pipelined.zrange(zset(topic), stillLive())
        return {
            val values = carried.get()
            here.get().map { PresenceEntry.of(it).member }.toSet().associateWith { member -> values[member.value]?.let { json.asA(it, type) } }
        }
    }

    override fun sweep(): Set<Topic> {
        val now = clock.millis().toDouble()
        val trimmed = redis.smembers(index).map(Topic::of).associateWith { topic ->
            pipelined.zremrangebyscore(zset(topic), Range.create(Double.NEGATIVE_INFINITY, now)) to pipelined.zcard(zset(topic))
        }
        val emptied = trimmed.filterValues { (_, remaining) -> remaining.get() == 0L }.keys
        if (emptied.isNotEmpty()) redis.srem(index, *emptied.map { it.value }.toTypedArray())
        val swept = trimmed.filterValues { (removed, _) -> removed.get() > 0 }.keys
        swept.forEach(::forget)
        return swept
    }

    private fun live(topic: Topic) = redis.zrange(zset(topic), stillLive()).map { PresenceEntry.of(it).member }.toSet()

    private fun stillLive() = ZRange.byScore(clock.millis().toDouble(), Double.POSITIVE_INFINITY)

    private fun forget(topic: Topic) {
        val here = live(topic).map { it.value }.toSet()
        val gone = redis.hkeys(values(topic)).filter { it !in here }
        if (gone.isNotEmpty()) redis.hdel(values(topic), *gone.toTypedArray())
    }

    override fun close() {
        connection.close()
        supplier.close()
    }

    private val index = "$prefix:keys"

    private fun zset(topic: Topic) = "$prefix:${topic.value}"

    private fun values(topic: Topic) = "$prefix:values:${topic.value}"
}

private class PresenceEntry private constructor(value: String) : StringValue(value) {
    val member get() = PresenceMember.of(value.substringAfter('|'))

    companion object : NonBlankStringValueFactory<PresenceEntry>(::PresenceEntry) {
        fun of(connection: ConnectionId, member: PresenceMember) = of("$connection|$member")
    }
}
