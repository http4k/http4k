package org.http4k.fanbelt.bus

internal object LocalTransport : Transport {
    override fun send(topics: Set<Topic>) {}

    override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {}

    override fun close() {}
}
