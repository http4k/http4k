package org.http4k.websocket

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.WsServerConfig
import org.http4k.server.asServer
import org.http4k.util.RetryRule
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.util.Random
import java.util.concurrent.LinkedBlockingQueue

private class RemoteClient(uri: Uri) : WebSocketClient(URI.create(uri.toString())) {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    override fun onMessage(bytes: ByteBuffer) {
        queue.add({ WsMessage(Body(bytes.array().inputStream())) })
    }

    override fun onOpen(handshakedata: ServerHandshake) {
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        queue.add({ null })
    }

    override fun onMessage(message: String) {
        queue.add({ WsMessage(message) })
    }

    override fun onError(ex: Exception?) {
    }
}

abstract class WebsocketServerContract(private val serverConfig: (Int) -> WsServerConfig, private val client: HttpHandler) {
    private lateinit var server: Http4kServer

    @Rule
    @JvmField
    var retryRule = RetryRule(5)

    private val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        val routes = routes(
            "/hello/{name}" bind { r: Request -> Response(OK).body(r.path("name")!!) }
        )
        val ws = websockets(
            "/hello" bind websockets(
                "/{name}" bind { ws: WebSocket ->
                    val name = ws.upgradeRequest.path("name")!!
                    ws.send(WsMessage(name))
                    ws.onMessage {
                        ws.send(WsMessage("goodbye $name".byteInputStream()))
                    }
                    ws.onClose { println("bob is closing") }
                }
            ))
        server = PolyHandler(routes, ws).asServer(serverConfig(port)).start()
    }

    @Test
    fun `can do standard http traffic`() {
        client(Request(GET, "http://localhost:$port/hello/bob")) shouldMatch hasBody("bob")
    }

    @Test
    fun `can send and receive messages from socket`() {
        val client = RemoteClient(Uri.of("ws://localhost:$port/hello/bob"))

        client.connectBlocking()
        client.send("hello")
        client.received.take(2).toList() shouldMatch equalTo(listOf(WsMessage("bob"), WsMessage("goodbye bob".byteInputStream())))
    }

    @After
    fun after() {
        server.stop()
    }

}