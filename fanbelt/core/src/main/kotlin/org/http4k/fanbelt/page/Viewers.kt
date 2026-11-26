package org.http4k.fanbelt.page

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class Viewers<Actor, Model> {
    private val listeners: MutableSet<Listener<Actor>> = CopyOnWriteArraySet()

    @Volatile
    private var last: Read<Model>? = null

    @Volatile
    private var version = 0L

    val watched get() = listeners.isNotEmpty()

    fun join(listener: Listener<Actor>) {
        listeners += listener
    }

    fun leave(listener: Listener<Actor>): Boolean {
        listeners -= listener
        return listeners.isEmpty()
    }

    fun catchUp(listener: Listener<Actor>, present: (Model) -> (Actor) -> Rendered): Boolean {
        val read = last ?: return false
        listener.send(present(read.value)(listener.actor), read.version)
        return true
    }

    fun differs(current: Model): Boolean {
        val previous = last
        return previous == null || previous.value != current
    }

    fun show(current: Model, present: (Model) -> (Actor) -> Rendered) {
        val read = Read(current, ++version)
        last = read
        val each = present(current)
        listeners.forEach { it.send(each(it.actor), read.version) }
    }

    fun unavailable(rendered: Rendered) {
        last = null
        val version = ++version
        listeners.forEach { it.send(rendered, version) }
    }
}

internal class Listener<out Actor>(val actor: Actor, private val send: (Rendered) -> Unit) {
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
