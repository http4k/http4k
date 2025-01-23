package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.routing.path
import org.http4k.routing.websocket.bind
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseWebsocketClientContract(private val serverConfig: PolyServerConfig): PortBasedTest {

    private lateinit var server: Http4kServer

    protected val port: Int
        get() = server.port()

    @BeforeEach
    fun before() {
        val ws = websockets(
            "/bin" bind { _: Request ->
                WsResponse { ws ->
                    ws.onMessage {
                        val content = it.body.stream.readBytes()
                        ws.send(WsMessage(content))
                        ws.close(WsStatus.NORMAL)
                    }
                }
            },

            "/headers" bind { req: Request ->
                WsResponse { ws ->
                    ws.onMessage {
                        req.headers.filter { it.first.startsWith("test") }.forEach { header ->
                            ws.send(WsMessage("${header.first}=${header.second}"))
                        }
                        ws.close(WsStatus.NORMAL)
                    }
                }
            },

            "/{name}" bind { req: Request ->
                WsResponse { ws ->
                    val name = req.path("name")!!
                    ws.send(WsMessage(name))
                    ws.onMessage {
                        ws.send(it)
                        ws.close(WsStatus.NORMAL)
                    }
                }
            },

            "/long-living/{name}" bind { req: Request ->
                WsResponse { ws ->
                    val name = req.path("name")!!
                    ws.send(WsMessage(name))
                    ws.onMessage {
                        ws.send(it)
                        // not sending close
                    }
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
