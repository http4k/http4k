package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.servlet.asServlet
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI


class Jetty(private val server: Server) : ServerConfig {
    constructor(port: Int = 8000) : this(Server(port))

    override fun toServer(handler: HttpHandler): Http4kServer {
        server.insertHandler(ServletContextHandler(SESSIONS).apply {
            addServlet(ServletHolder(handler.asServlet()), "/*")
        })

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop() = server.stop()
        }
    }
}

class WsJetty(private val server: Server) {
    constructor(port: Int = 8000) : this(Server(port))

    fun toServer(handler: HttpHandler, a: WebsocketRouter): Http4kServer {

        server.insertHandler(ServletContextHandler(SESSIONS).apply {
            addServlet(ServletHolder(handler.asServlet()), "/*")
        })
        server.insertHandler(object : WebSocketHandler() {
            override fun configure(factory: WebSocketServletFactory) {
                factory.policy.idleTimeout = 10000
                factory.setCreator { req, _ ->
                    println("hello")
                    a.match(req.asHttp4kRequest())?.let { Http4kWebsocketEndpoint(it) }
                }
            }
        })

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop() = server.stop()
        }
    }
}

object EventClient {
    fun bob() {
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
//        a.close()
    }
}

fun main(args: Array<String>) {
    val app = { r: Request -> Response(OK) }
    val server = WsJetty(8000).toServer(app, object : WebsocketRouter {
        override fun match(request: Request): WSocket? {
            return object: WSocket {
                override fun onError(throwable: Throwable, session: WsSession) {
                }

                override fun onClose(status: Status, session: WsSession) {
                }

                override fun onMessage(body: Body, session: WsSession) {
                    println("i got " + body)
                    session(body)
                    session.close()
                }
            }
        }

    }).start()

    EventClient.bob()

    server.stop()
}