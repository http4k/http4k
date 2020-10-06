package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.WebsocketClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.WsServerConfig
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

abstract class WebsocketServerContract(private val serverConfig: (Int) -> WsServerConfig, private val client: HttpHandler) {
    private lateinit var server: Http4kServer

    private val port by lazy { server.port() }

    private val lens = WsMessage.string().map(String::toInt).toLens()

    @BeforeEach
    fun before() {
        val routes = routes(
            "/hello/{name}" bind { r: Request -> Response(OK).body(r.path("name")!!) }
        )
        val ws = websockets(
            "/hello" bind websockets(
                "/{name}" bind { ws: Websocket ->
                    val name = ws.upgradeRequest.path("name")!!
                    ws.send(WsMessage(name))
                    ws.onMessage {
                        ws.send(WsMessage("goodbye $name".byteInputStream()))
                    }
                    ws.onClose { println("bob is closing") }
                }
            ),
            "/errors" bind { ws: Websocket ->
                ws.onMessage {
                    lens.extract(it)
                }
                ws.onError { ws.send(WsMessage(it.localizedMessage)) }
            },
            "/queries" bind { ws: Websocket ->
                ws.onMessage {
                    ws.send(WsMessage(ws.upgradeRequest.query("query") ?: "not set"))
                }
                ws.onError { ws.send(WsMessage(it.localizedMessage)) }
            })

        server = PolyHandler(routes, ws).asServer(serverConfig(0)).start()
    }

    @AfterEach
    fun after() {
        server.stop()
    }

    @Test
    fun `can do standard http traffic`() {
        assertThat(client(Request(GET, "http://localhost:$port/hello/bob")), hasBody("bob"))
    }

    @Test
    fun `can send and receive messages from socket`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/hello/bob"))

        client.send(WsMessage("hello"))
        assertThat(client.received().take(2).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("goodbye bob".byteInputStream()))))
    }

    @Test
    fun `errors are propagated to the "on error" handler`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/errors"))
        client.send(WsMessage("hello"))
        assertThat(client.received().take(1).toList(), equalTo(listOf(WsMessage("websocket 'message' must be object"))))
    }

    @Test
    fun `should correctly set query parameters on upgrade request passed into the web socket`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/queries?query=foo"))
        client.send(WsMessage("hello"))
        assertThat(client.received().take(1).toList(), equalTo(listOf(WsMessage("foo"))))
    }

    @Test
    fun `can connect with non-blocking client`() {
        val client = WebsocketClient.nonBlocking(Uri.of("ws://localhost:$port/hello/bob"))
        val latch = CountDownLatch(1)
        client.onMessage {
            latch.countDown()
        }

        latch.await()
    }
}
