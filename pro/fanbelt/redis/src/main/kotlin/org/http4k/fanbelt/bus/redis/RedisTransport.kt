/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.redis

import io.lettuce.core.RedisChannelHandler
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisConnectionStateListener
import io.lettuce.core.pubsub.RedisPubSubAdapter
import org.http4k.core.Uri
import org.http4k.events.Events
import org.http4k.fanbelt.bus.BackplaneDisconnected
import org.http4k.fanbelt.bus.BackplaneMessage
import org.http4k.fanbelt.bus.ChannelName
import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.Transport
import org.http4k.fanbelt.redis.RedisSupplier
import java.net.SocketAddress

class RedisTransport internal constructor(private val supplier: RedisSupplier, private val events: Events, private val channel: ChannelName) : Transport {
    constructor(uri: Uri, events: Events, channel: ChannelName = ChannelName.default) : this(RedisSupplier.Owned(uri), events, channel)

    constructor(client: RedisClient, events: Events, channel: ChannelName = ChannelName.default) : this(RedisSupplier.Borrowed(client), events, channel)

    @Volatile
    private var connected: Connected? = null

    private class Connected(client: RedisClient) {
        val publisher = client.connect()
        val subscriber = client.connectPubSub()
    }

    override fun send(message: BackplaneMessage) {
        started().publisher.async().publish(channelFor(message.interest), message.value)
    }

    override fun interested(interest: Interest) {
        started().subscriber.sync().subscribe(channelFor(interest))
    }

    override fun uninterested(interest: Interest) {
        started().subscriber.async().unsubscribe(channelFor(interest))
    }

    private fun started() = connected ?: error("RedisTransport not started")

    private fun channelFor(interest: Interest) = "${channel.value}:${interest.value}"

    override fun start(received: (BackplaneMessage) -> Unit, reconnected: () -> Unit) {
        supplier.start()
        val connection = Connected(supplier.client)
        connected = connection
        connection.subscriber.addListener(object : RedisConnectionStateListener {
            override fun onRedisDisconnected(connection: RedisChannelHandler<*, *>) {
                events(BackplaneDisconnected(IllegalStateException("redis connection lost")))
            }

            override fun onRedisConnected(connection: RedisChannelHandler<*, *>, socketAddress: SocketAddress) = reconnected()
        })
        connection.subscriber.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) = received(BackplaneMessage.of(message))
        })
    }

    override fun close() {
        val connection = connected ?: return
        connected = null
        connection.subscriber.close()
        connection.publisher.close()
        supplier.close()
    }
}
