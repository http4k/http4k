/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.memory

import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.Transport
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryBroker {
    private val connected = CopyOnWriteArrayList<(Topic) -> Unit>()

    fun transport(): Transport = object : Transport {
        private val interests = ConcurrentHashMap.newKeySet<Interest>()
        private var receiver: ((Topic) -> Unit)? = null

        override fun send(topic: Topic) = connected.forEach { it(topic) }

        override fun start(received: (Topic) -> Unit, reconnected: () -> Unit) {
            val filtered = { topic: Topic -> if (Interest.of(topic) in interests) received(topic) }
            receiver = filtered
            connected += filtered
        }

        override fun interested(interest: Interest) {
            interests += interest
        }

        override fun uninterested(interest: Interest) {
            interests -= interest
        }

        override fun close() {
            receiver?.let(connected::remove)
        }
    }
}
