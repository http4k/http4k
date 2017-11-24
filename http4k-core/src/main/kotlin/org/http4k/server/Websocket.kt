package org.http4k.server

import org.http4k.core.Body
import java.io.Closeable

interface Websocket : Closeable {
    operator fun invoke(message: WsMessage)
}

class WsBuilder {
    fun close() {
        println("closing")
    }

    fun send(message: WsMessage) = println(message)
    lateinit var onMessage: (WsMessage) -> Unit
    lateinit var onClose: () -> Unit
}

fun websocket(fn: WsBuilder.() -> Unit) = WsBuilder().apply(fn).run {
    val configured = this
    object : Websocket {
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
        send(WsMessage(Body("bar")))
        close()
    }
}

fun main(args: Array<String>) {

    WsMessage.binary().toLens()
    websocket(WsMessage(Body("foo")))
}