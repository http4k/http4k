/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock

/**
 * Locks shared by every node, taken without waiting: tryLock gives null when anyone holds the name, and the lock is
 * held until closed. A node that dies lets go of its locks.
 */
fun interface Locks {
    fun tryLock(name: String): AutoCloseable?
}
