/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus.memory

import org.http4k.fanbelt.bus.BackplaneMessage
import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.Transport
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryBroker {
    private val connected = CopyOnWriteArrayList<(BackplaneMessage) -> Unit>()

    fun transport(): Transport = object : Transport {
        private val interests = ConcurrentHashMap.newKeySet<Interest>()
        private var receiver: ((BackplaneMessage) -> Unit)? = null

        override fun send(message: BackplaneMessage) = connected.forEach { it(message) }

        override fun start(received: (BackplaneMessage) -> Unit, reconnected: () -> Unit) {
            val filtered = { message: BackplaneMessage -> if (message.interest in interests) received(message) }
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
