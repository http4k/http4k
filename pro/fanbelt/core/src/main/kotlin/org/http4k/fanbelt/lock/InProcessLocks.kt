/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock

import java.util.concurrent.ConcurrentHashMap

/**
 * Locks held in this process: enough for one node, or for storage only one process uses.
 */
class InProcessLocks : Locks {
    private val held = ConcurrentHashMap.newKeySet<String>()

    override fun tryLock(name: String) = if (held.add(name)) AutoCloseable { held.remove(name) } else null
}
