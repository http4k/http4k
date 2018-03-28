package org.http4k.websocket

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
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
import org.http4k.util.RetryRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Random

abstract class WebsocketServerContract(private val serverConfig: (Int) -> WsServerConfig, private val client: HttpHandler) {
    private lateinit var server: Http4kServer

    @Rule
    @JvmField
    var retryRule = RetryRule.CI

    private val port = Random().nextInt(1000) + 8000

    private val lens = WsMessage.string().map(String::toInt).toLens()

    @Before
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
            })
        server = PolyHandler(routes, ws).asServer(serverConfig(port)).start()
    }

    @After
    fun after() {
        server.stop()
    }

    @Test
    fun `can do standard http traffic`() {
        client(Request(GET, "http://localhost:$port/hello/bob")) shouldMatch hasBody("bob")
    }

    @Test
    fun `can send and receive messages from socket`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/hello/bob"))

        client.send(WsMessage("hello"))
        client.received().take(2).toList() shouldMatch equalTo(listOf(WsMessage("bob"), WsMessage("goodbye bob".byteInputStream())))
    }

    @Test
    fun `errors are propagated to the "on error" handler`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/errors"))
        client.send(WsMessage("hello"))
        client.received().take(1).toList() shouldMatch equalTo(listOf(WsMessage("websocket 'message' must be object")))
    }
}