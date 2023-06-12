package org.http4k.websocket

import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseWebsocketClientContract(private val serverConfig: PolyServerConfig) {

    private lateinit var server: Http4kServer

    protected val port: Int
        get() = server.port()

    @BeforeEach
    fun before() {
        val ws = websockets(
            "/bin" bind { ws: Websocket ->
                ws.onMessage {
                    val content = it.body.stream.readBytes()
                    ws.send(WsMessage(content.inputStream()))
                    ws.close(WsStatus.NORMAL)
                }
            },

            "/headers" bind { ws: Websocket ->
                ws.onMessage {
                    ws.upgradeRequest.headers.filter { it.first.startsWith("test") }.forEach { header ->
                        ws.send(WsMessage("${header.first}=${header.second}"))
                    }
                    ws.close(WsStatus.NORMAL)
                }
            },

            "/{name}" bind { ws: Websocket ->
                val name = ws.upgradeRequest.path("name")!!
                ws.send(WsMessage(name))
                ws.onMessage {
                    ws.send(it)
                    ws.close(WsStatus.NORMAL)
                }
            },

            "/long-living/{name}" bind { ws: Websocket ->
                val name = ws.upgradeRequest.path("name")!!
                ws.send(WsMessage(name))
                ws.onMessage {
                    ws.send(it)
                    // not sending close
                }
            }
        )
        server = ws.asServer(serverConfig).start()
    }

    @AfterEach
    fun after() {
        server.stop()
    }
}
