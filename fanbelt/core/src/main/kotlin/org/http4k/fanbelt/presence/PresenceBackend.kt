package org.http4k.fanbelt.presence

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.Lifecycle
import java.time.Duration

data class PresenceConnection(val topic: Topic, val id: ConnectionId, val member: PresenceMember)

/**
 * Where presence is kept, shared by every node.
 */
interface PresenceBackend : Lifecycle {
    fun join(connections: List<PresenceConnection>, ttl: Duration)
    fun leave(connection: PresenceConnection)
    fun set(topic: Topic, member: PresenceMember, value: String)
    fun present(topic: Topic): Map<PresenceMember, String?>
    fun present(topics: Set<Topic>): Map<Topic, Map<PresenceMember, String?>> = topics.associateWith(::present)
    fun sweep(): Set<Topic>
}
