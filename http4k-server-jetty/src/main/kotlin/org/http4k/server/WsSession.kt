package org.http4k.server

import org.http4k.core.Body
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

interface WsSession : Closeable, (Body) -> Unit {
    companion object {
        operator fun invoke() = MemoryWsSession()
    }
}

class MemoryWsSession : WsSession {

    private val queue = LinkedBlockingQueue<() -> Body?>()

    fun stream() = generateSequence { queue.take()() }

    override fun invoke(p1: Body) {
        queue.add { p1 }
    }

    override fun close() {
        queue.add { null }
    }
}

fun main(args: Array<String>) {
    val a = WsSession()

    a.invoke(Body("1"))
    a.invoke(Body("2"))
    a.close()

    a.stream().forEach { println(it) }
}