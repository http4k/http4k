/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.events.Events
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

internal class Subscriber(private val executor: Executor, private val events: Events, private val onChange: () -> Unit) {
    private val dirty = AtomicBoolean(false)
    private val draining = AtomicBoolean(false)

    fun markDirty() {
        dirty.set(true)
        if (draining.compareAndSet(false, true)) executor.execute(::drain)
    }

    private fun drain() {
        do {
            while (dirty.getAndSet(false)) runCatching(onChange).onFailure { events(SubscriberFailed(it)) }
            draining.set(false)
        } while (dirty.get() && draining.compareAndSet(false, true))
    }
}
