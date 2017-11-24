package org.http4k.server

import java.io.Closeable

interface Websocket : Closeable {
    operator fun invoke(message: WsMessage)
}

data class WsMessage(val content: String)

class WsBuilder {
    fun close(): Unit = println("closing")
    fun send(message: WsMessage) = println(message)
    lateinit var onMessage: (WsMessage) -> Unit
    lateinit var onClose: () -> Unit
}

fun websocket(fn: WsBuilder.() -> Unit) = WsBuilder().apply(fn).run {
    val configured = this
    object : Websocket {
        protected fun respond(message: WsMessage): Unit = println(message)
        override fun invoke(message: WsMessage) = configured.onMessage(message)
        override fun close() {
            close()
            configured.onClose()
        }
    }
}

interface WS : Closeable {
    operator fun invoke(m: WsMessage)

    companion object {
        operator fun invoke(): WS = InMemoryWebsocket()
    }
}

internal data class InMemoryWebsocket internal constructor(val messages: MutableList<WsMessage> = mutableListOf()) : WS {

    private var closed = false

    override fun invoke(m: WsMessage) {
        if (closed) throw RuntimeException("socket is closed")
        messages += m
    }

    override fun close() {
        closed = true
    }
}

val websocket: Websocket = websocket {
    onMessage = {
        println("foo")
        send(WsMessage("bar"))
        close()
    }
}

fun main(args: Array<String>) {
    websocket(WsMessage("hello"))
}