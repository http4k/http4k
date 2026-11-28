package org.http4k.fanbelt.bus

import org.http4k.fanbelt.infrastructure.Lifecycle

/**
 * Tells subscribers that a topic has changed.
 */
interface Bus : Lifecycle {
    fun publish(topic: Topic) = publish(setOf(topic))
    fun publish(topics: Set<Topic>)
    fun subscribe(topics: Set<Topic>, onChange: () -> Unit): Subscription
}
