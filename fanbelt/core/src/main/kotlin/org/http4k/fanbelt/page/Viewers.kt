/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class Viewers<Model> {
    private val listeners: MutableSet<Listener> = CopyOnWriteArraySet()

    @Volatile
    private var last: Read<Model>? = null

    @Volatile
    private var version = 0L

    val watched get() = listeners.isNotEmpty()

    fun join(listener: Listener) {
        listeners += listener
    }

    fun leave(listener: Listener): Boolean {
        listeners -= listener
        return listeners.isEmpty()
    }

    fun catchUp(listener: Listener, present: (Model) -> (Request) -> Rendered): Boolean {
        val read = last ?: return false
        listener.send(present(read.value)(listener.request), read.version)
        return true
    }

    fun differs(current: Model): Boolean {
        val previous = last
        return previous == null || previous.value != current
    }

    fun show(current: Model, present: (Model) -> (Request) -> Rendered) {
        val read = Read(current, ++version)
        last = read
        val each = present(current)
        listeners.forEach { it.send(each(it.request), read.version) }
    }

    fun unavailable(rendered: Rendered) {
        last = null
        val version = ++version
        listeners.forEach { it.send(rendered, version) }
    }
}

internal class Listener(val request: Request, private val send: (Rendered) -> Unit) {
    private val lock = ReentrantLock()

    private var sent = 0L

    fun send(rendered: Rendered, version: Long) = lock.withLock {
        if (version > sent) {
            sent = version
            send(rendered)
        }
    }
}

private class Read<Model>(val value: Model, val version: Long)
