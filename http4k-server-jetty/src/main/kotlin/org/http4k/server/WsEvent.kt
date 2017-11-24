package org.http4k.server

class Session : AutoCloseable {
    override fun close() {
        TODO("not implemented")
    }

    fun send(message: WsMessage): Unit = TODO()
}

sealed class WsEvent<out T>

object CONNECT : WsEvent<Session>()

object MESSAGE : WsEvent<WsMessage>()

object ERROR : WsEvent<Exception>()

object CLOSE : WsEvent<Unit>()