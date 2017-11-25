package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.Closeable
import java.lang.Exception
import java.net.URI

object EventClient {
    fun bob(): Closeable {
        val a = object : WebSocketClient(URI.create("ws://localhost:8000/bob")) {
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
        a.send("sending..")
        return Closeable { a.close() }
    }
}

fun main(args: Array<String>) {
    val app = { r: Request -> Response(Status.OK).body("hiya world") }
    val server = WsJetty(8000).toServer(app, object : WebsocketRouter {
        override fun match(request: Request): WSocket? {
            return object : WSocket {
                override fun onError(throwable: Throwable, session: WsSession) {
                }

                override fun onClose(status: Status, session: WsSession) {
                }

                override fun onMessage(body: Body, session: WsSession) {
                    println("i got " + body)
                    session(Body("sendnign this back"))
                }
            }
        }

    }).start()

    println(ApacheClient()(Request(Method.GET, "http://localhost:8000/hello")))

    val a = EventClient.bob()

    server.stop()

    a.close()
}