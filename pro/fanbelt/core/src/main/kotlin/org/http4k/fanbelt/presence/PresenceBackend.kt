/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.Lifecycle
import java.time.Duration
import kotlin.reflect.KClass

data class PresenceConnection(val topic: Topic, val id: ConnectionId, val member: PresenceMember)

/**
 * Where presence is kept, so every node sees the same members: each connection's lease with its expiry, and the value a
 * member carries. sweep removes expired leases and returns the topics that changed, for publishing.
 */
interface PresenceBackend : Lifecycle {
    fun join(connections: List<PresenceConnection>, ttl: Duration)
    fun leave(connection: PresenceConnection)
    fun set(topic: Topic, member: PresenceMember, value: Any)
    fun <V : Any> present(topic: Topic, type: KClass<V>): Map<PresenceMember, V?>
    fun present(topics: Map<Topic, KClass<out Any>>): Map<Topic, Map<PresenceMember, Any?>> = topics.mapValues { (topic, type) -> present(topic, type) }
    fun sweep(): Set<Topic>
}
