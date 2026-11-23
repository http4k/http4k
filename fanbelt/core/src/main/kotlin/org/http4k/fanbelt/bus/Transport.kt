package org.http4k.fanbelt.bus

/**
 * Carries topics between nodes for a Backplane.
 */
interface Transport : AutoCloseable {
    fun send(topics: Set<Topic>)

    fun start(received: (Topic) -> Unit, reconnected: () -> Unit)

    fun interested(interest: Interest) {}

    fun uninterested(interest: Interest) {}
}
