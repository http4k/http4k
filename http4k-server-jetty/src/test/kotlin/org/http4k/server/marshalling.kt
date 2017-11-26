package org.http4k.server

import org.http4k.websocket.WsMessage
import org.http4k.websocket.string

data class Wrapper(val v: Int)

fun main(args: Array<String>) {
    val session = WsSession()

    val body = WsMessage.string().map { Wrapper(it.toInt()) }.toLens()

    session(WsMessage("1"))
    session(WsMessage("2"))
    session.close()

    session.received.forEach { println(body(it)) }
}