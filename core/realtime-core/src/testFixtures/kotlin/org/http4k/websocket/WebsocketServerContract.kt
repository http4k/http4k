package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.http4k.base64Encode
import org.http4k.client.WebsocketClient
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.lens.string
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.websocket.bind
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration
import java.util.concurrent.CountDownLatch
import org.http4k.routing.bind as hbind

abstract class WebsocketServerContract(
    private val serverConfig: (Int) -> PolyServerConfig,
    private val client: HttpHandler,
    private val httpSupported: Boolean = true
) : PortBasedTest {
    private lateinit var server: Http4kServer

    private val port by lazy { server.port() }

    private val lens = WsMessage.string().map(String::toInt).toLens()

    private val err = ByteArrayOutputStream()
    private val sysErr = System.err

    init {
        System.setErr(PrintStream(err))
    }

    @BeforeEach
    fun before() {
        val routes = routes(
            "/hello/{name}" hbind { r: Request -> Response(OK).body(r.path("name")!!) }
        )
        val ws = websockets(
            "/noclose" bind {
                WsResponse { ws ->
                    ws.send(WsMessage("event"))
                }
            },
            "/hello" bind websockets(
                "/{name}" bind { req: Request ->
                    WsResponse { ws ->
                        val name = req.path("name")!!
                        ws.send(WsMessage(name))
                        ws.onMessage { ws.send(WsMessage("goodbye $name")) }
                        ws.onClose { println("$name is closing") }
                    }
                }
            ),
            "/errors" bind { _: Request ->
                WsResponse { ws ->
                    ws.onMessage { lens(it) }
                    ws.onError {
                        ws.send(WsMessage(it.localizedMessage))
                    }
                }
            },
            "/queries" bind { req: Request ->
                WsResponse { ws ->
                    ws.onMessage { ws.send(WsMessage(req.query("query") ?: "not set")) }
                    ws.onError { ws.send(WsMessage(it.localizedMessage)) }
                }
            },
            "/echo" bind { _: Request ->
                WsResponse { ws ->
                    ws.onMessage { ws.send(it) }
                }
            },
            "/bin" bind { _ ->
                WsResponse { ws ->
                    ws.onMessage { message ->
                        val content = message.body.stream.readBytes()
                        ws.send(WsMessage(content))
                    }
                }
            })

        server = PolyHandler(routes.takeIf { httpSupported }, ws).asServer(serverConfig(0)).start()
    }

    @AfterEach
    fun after() {
        server.stop()
        sysErr.print(err.toByteArray())
        System.setErr(sysErr)
    }

    @Test
    fun `can do standard http traffic`() {
        if (!httpSupported) return
        assertThat(client(Request(GET, "http://localhost:$port/hello/bob")), hasBody("bob"))
    }

    @Test
    fun `does not error when we do not call close`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${port}/noclose"))
        assertThat(
            client.received().take(1).toList(),
            equalTo(listOf(WsMessage("event")))
        )
        val syserr = String(err.toByteArray()).lowercase()
        assertThat(syserr, !containsSubstring("exception"))
        assertThat(syserr, !containsSubstring("error"))
        assertThat(syserr, !containsSubstring("warning"))
    }

    @Test
    fun `can send and receive text messages from socket`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/hello/bob"))
        client.send(WsMessage("hello"))

        val messages = client.received().take(2).toList()

        messages.first().also { message ->
            assertThat(message.mode, equalTo(WsMessage.Mode.Text))
            assertThat(message.bodyString(), equalTo("bob"))
        }

        messages.last().also { message ->
            assertThat(message.mode, equalTo(WsMessage.Mode.Text))
            assertThat(message.bodyString(), equalTo("goodbye bob"))
        }
    }

    @Test
    fun `can send and receive binary messages from socket - MemoryBody`() {
        val content = javaClass.classLoader.getResourceAsStream("org/http4k/websocket/sample_2k.png")!!.readBytes()

        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bin"))
        client.send(WsMessage(MemoryBody(content), WsMessage.Mode.Binary))

        val (message) = client.received().take(1).toList()
        assertThat(message.mode, equalTo(WsMessage.Mode.Binary))
        assertThat(message.body.stream.readBytes().base64Encode(), equalTo(content.base64Encode()))
    }

    @Test
    fun `can send and receive binary messages from socket - StreamBody`() {
        val content = javaClass.classLoader.getResourceAsStream("org/http4k/websocket/sample_2k.png")!!.readBytes()

        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bin"))
        client.send(WsMessage(StreamBody(content.inputStream()), WsMessage.Mode.Binary))

        val (message) = client.received().take(1).toList()
        assertThat(message.mode, equalTo(WsMessage.Mode.Binary))
        assertThat(message.body.stream.readBytes().base64Encode(), equalTo(content.base64Encode()))
    }

    @Test
    fun `errors are propagated to the 'on error' handler`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/errors"))
        client.send(WsMessage("hello"))
        assertThat(
            client.received().take(1).toList(),
            equalTo(listOf(WsMessage("websocket 'message' must be object")))
        )
    }

    @Test
    fun `should propagate close on client close`() {
        val latch = CountDownLatch(1)
        var closeStatus: WsStatus? = null

        val server = websockets(
            "/closes" bind { _: Request ->
                WsResponse { ws ->
                    ws.onClose {
                        closeStatus = it
                        latch.countDown()
                    }
                }
            }).asServer(serverConfig(0)).start()
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${server.port()}/closes"))
        client.close()

        latch.await()
        assertThat(closeStatus, present())
        server.close()
    }

    @Test
    fun `should propagate close on server close`() {
        val latch = CountDownLatch(1)
        var closeStatus: WsStatus? = null

        val server = websockets(
            "/closes" bind { _: Request ->
                WsResponse { ws ->
                    ws.onMessage {
                        ws.close()
                    }
                    ws.onClose {
                        closeStatus = it
                        latch.countDown()
                    }
                }
            }).asServer(serverConfig(0)).start()
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${server.port()}/closes"))
        client.send(WsMessage("message"))

        latch.await()
        assertThat(closeStatus, present())
        client.close()
        server.close()
    }

    @Test
    open fun `should propagate close on server stop`() {
        val latch = CountDownLatch(1)
        var closeStatus: WsStatus? = null

        val server = websockets(
            "/closes" bind { _: Request ->
                WsResponse { ws ->
                    ws.onClose {
                        closeStatus = it
                        latch.countDown()
                    }
                }
            }).asServer(serverConfig(0)).start()
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${server.port()}/closes"))
        client.send(WsMessage("message"))
        server.close()

        latch.await()
        assertThat(closeStatus, present())
        client.close()
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

    @Test
    fun `should fail on invalid url`() {
        assertThat({
            val client = WebsocketClient.blocking(
                Uri.of("ws://localhost:$port/aaa"),
                timeout = Duration.ZERO
            )

            client.send(WsMessage("hello"))
        }, throws<WebsocketNotConnectedException>())
    }

    @Test
    fun `can send and receive multi-frame messages from socket`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/echo"))

        val longMessage = WsMessage((1..10000).joinToString("") { "a" })
        client.send(longMessage)

        val anotherMessage = WsMessage("another message")
        client.send(anotherMessage)

        assertThat(client.received().take(2).toList(), equalTo(listOf(longMessage, anotherMessage)))
    }
}
