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

internal class SharedPresence(
    private val backend: PresenceBackend,
    private val bus: Bus,
    private val scheduler: ScheduledExecutorService,
    private val random: Random,
    private val events: Events,
    private val ttl: Duration,
    private val heartbeat: Duration,
) : Presence {
    init {
        require(ttl > heartbeat.multipliedBy(2)) {
            "FANBELT_PRESENCE_TTL ($ttl) must be more than twice FANBELT_PRESENCE_HEARTBEAT ($heartbeat), or members drop out between renewals"
        }
    }

    private val mine = ConcurrentHashMap<ConnectionId, PresenceConnection>()

    @Volatile
    private var renewal: ScheduledFuture<*>? = null

    override fun start() {
        backend.start()
        renewal = scheduler.scheduleWithFixedDelay(::renew, heartbeat.toMillis(), heartbeat.toMillis(), MILLISECONDS)
    }

    override fun present(topics: Set<Topic>) = backend.present(topics)

    override fun join(topic: Topic, member: PresenceMember): Subscription {
        check(renewal != null) { "SharedPresence isn't started: add it to the App's Node" }
        val id = ConnectionId.random(random)
        val connection = PresenceConnection(topic, id, member)
        mine[id] = connection
        runCatching { backend.join(listOf(connection), ttl) }
            .onSuccess { bus.publish(topic) }
            .onFailure {
                failing = true
                events(PresenceJoinFailed(topic, it))
            }
        return Subscription {
            mine.remove(id)
            backend.leave(connection)
            bus.publish(topic)
        }
    }

    override fun set(topic: Topic, member: PresenceMember, value: String) {
        backend.set(topic, member, value)
        bus.publish(topic)
    }

    override fun close() {
        renewal?.cancel(false)
        backend.close()
    }

    @Volatile
    private var failing = false

    private fun renew() {
        val renewed = attempt { backend.join(mine.values.toList(), ttl) } and attempt { bus.publish(backend.sweep()) }
        if (renewed && failing) {
            events(PresenceRecovered)
            bus.publish(mine.values.map { it.topic }.toSet())
        }
        failing = !renewed
    }

    private fun attempt(block: () -> Unit) = runCatching(block).onFailure { events(PresenceRenewalFailed(it)) }.isSuccess
}
