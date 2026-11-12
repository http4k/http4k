/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import dev.forkhandles.values.random
import org.http4k.events.Events
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import java.time.Duration
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.reflect.KClass

internal class SharedPresence(
    private val backend: PresenceBackend,
    private val bus: Bus,
    private val scheduler: ScheduledExecutorService,
    private val random: Random,
    private val events: Events,
    private val ttl: Duration,
    private val heartbeat: Duration,
) : Presence {

    private val mine = ConcurrentHashMap<ConnectionId, PresenceConnection>()

    @Volatile
    private var renewal: ScheduledFuture<*>? = null

    override fun start() {
        backend.start()
        renewal = scheduler.scheduleWithFixedDelay(::renew, heartbeat.toMillis(), heartbeat.toMillis(), MILLISECONDS)
    }

    override fun present(topics: Map<Topic, KClass<out Any>>) = backend.present(topics)

    override fun join(topic: Topic, member: PresenceMember): Subscription {
        check(renewal != null) { "SharedPresence isn't started: add it to the App's Node" }
        val id = ConnectionId.random(random)
        val connection = PresenceConnection(topic, id, member)
        mine[id] = connection
        backend.join(listOf(connection), ttl)
        bus.publish(topic)
        return Subscription {
            mine.remove(id)
            backend.leave(connection)
            bus.publish(topic)
        }
    }

    override fun set(topic: Topic, member: PresenceMember, value: Any) {
        backend.set(topic, member, value)
        bus.publish(topic)
    }

    override fun close() {
        renewal?.cancel(false)
        backend.close()
    }

    private var failing = false

    private fun renew() {
        val renewed = attempt { backend.join(mine.values.toList(), ttl) } and attempt { bus.publish(backend.sweep()) }
        if (renewed && failing) events(PresenceRecovered)
        failing = !renewed
    }

    private fun attempt(block: () -> Unit) = runCatching(block).onFailure { events(PresenceRenewalFailed(it)) }.isSuccess
}
