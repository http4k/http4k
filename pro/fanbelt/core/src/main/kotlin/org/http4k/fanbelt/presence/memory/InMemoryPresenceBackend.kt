/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence.memory

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.presence.ConnectionId
import org.http4k.fanbelt.presence.PresenceBackend
import org.http4k.fanbelt.presence.PresenceConnection
import org.http4k.fanbelt.presence.PresenceMember
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class InMemoryPresenceBackend(private val clock: Clock) : PresenceBackend {
    private data class Entry(val member: PresenceMember, val expiresAt: Instant)

    private val topics = ConcurrentHashMap<Topic, ConcurrentHashMap<ConnectionId, Entry>>()
    private val values = ConcurrentHashMap<Topic, ConcurrentHashMap<PresenceMember, Any>>()

    override fun join(connections: List<PresenceConnection>, ttl: Duration) {
        val expiresAt = clock.instant() + ttl
        connections.forEach { connection -> topics.compute(connection.topic) { _, entries -> (entries ?: ConcurrentHashMap()).also { it[connection.id] = Entry(connection.member, expiresAt) } } }
    }

    override fun leave(connection: PresenceConnection) {
        topics.computeIfPresent(connection.topic) { _, entries -> entries.also { it.remove(connection.id) }.takeIf { it.isNotEmpty() } }
        forget(connection.topic)
    }

    override fun set(topic: Topic, member: PresenceMember, value: Any) {
        if (member in live(topic)) values.compute(topic) { _, carried -> (carried ?: ConcurrentHashMap()).also { it[member] = value } }
    }

    override fun <V : Any> present(topic: Topic, type: KClass<V>): Map<PresenceMember, V?> {
        val carried = values[topic].orEmpty()
        return live(topic).associateWith { member -> carried[member]?.let(type.javaObjectType::cast) }
    }

    override fun sweep(): Set<Topic> {
        val now = clock.instant()
        val swept = topics.filter { (_, entries) -> entries.values.removeIf { it.expiresAt <= now } }.keys.toSet()
        swept.forEach { topic -> topics.computeIfPresent(topic) { _, entries -> entries.takeIf { it.isNotEmpty() } } }
        swept.forEach(::forget)
        return swept
    }

    private fun live(topic: Topic): Set<PresenceMember> {
        val now = clock.instant()
        return topics[topic].orEmpty().values.filter { it.expiresAt > now }.map { it.member }.toSet()
    }

    private fun forget(topic: Topic) {
        values.computeIfPresent(topic) { _, carried -> carried.also { it.keys.retainAll(live(topic)) }.takeIf { it.isNotEmpty() } }
    }
}
