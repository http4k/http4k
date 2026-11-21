/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.sse.Sse

internal class Connection(sse: Sse) {
    private val held = ArrayDeque<AutoCloseable>()
    private var closed = false

    init {
        sse.onClose(::close)
    }

    fun own(resource: AutoCloseable) {
        val kept = synchronized(this) { !closed && held.add(resource) }
        if (!kept) resource.close()
    }

    private fun close() {
        val owned = synchronized(this) {
            closed = true
            held.toList().also { held.clear() }
        }
        owned.asReversed().forEach { runCatching { it.close() } }
    }
}
