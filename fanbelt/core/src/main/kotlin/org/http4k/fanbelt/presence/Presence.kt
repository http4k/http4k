package org.http4k.fanbelt.presence

import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.Lifecycle

/**
 * Leased membership per topic, shared by every node.
 */
internal interface Presence : Lifecycle {
    fun present(topics: Set<Topic>): Map<Topic, Map<PresenceMember, String?>>

    fun join(topic: Topic, member: PresenceMember): Subscription

    fun set(topic: Topic, member: PresenceMember, value: String)
}
