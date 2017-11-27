package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.websocket.WsMessage
import org.http4k.websocket.bind
import org.http4k.websocket.websocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.Closeable
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

fun clientAction(path: String): Closeable {
    val a = object : WebSocketClient(URI.create("ws://localhost:8000/$path")) {

        override fun onMessage(bytes: ByteBuffer) {
            println("I got binary back: " + String(bytes.array()))
        }

        override fun onOpen(handshakedata: ServerHandshake) {
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
        }

        override fun onMessage(message: String?) {
            println("I got back: " + message)
        }

        override fun onError(ex: Exception?) {
        }
    }

    a.connectBlocking()
    a.send("sending.. to " + path)
    return Closeable { a.close() }
}


val app = { r: Request -> Response(Status.OK).body("hiya world") }

val webSocketHandler = websocket(
    "/hello" bind websocket(
        "/bob" bind { ws ->
            println("hello bob")
            ws.onMessage {
                println("bob got " + it)
                ws(WsMessage("bob sending this back".byteInputStream()))
            }
        },
        "/" bind { ws ->
            println("hello")
            ws
                .onMessage {
                    println("i got " + it)
                    ws(WsMessage("sending this back".byteInputStream()))
                }
                .onMessage {
                    println("i also got " + it)
                }
        }
    )
)

fun main(args: Array<String>) {
    val server = WsJetty(8000).toServer(app, webSocketHandler).start()

    println(ApacheClient()(Request(Method.GET, "http://localhost:8000/hello")))

    clientAction("hello")
    clientAction("hello/bob")
}
