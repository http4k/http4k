package org.http4k.sse

import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.util.proxy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.Executors


abstract class SseServerContract(private val serverConfig: (Int) -> PolyServerConfig, private val client: HttpHandler) {
    private lateinit var server: Http4kServer

    private val port by lazy { server.port() }

    @BeforeEach
    fun before() {
        val http = routes(
            "/hello/{name}" bind { r: Request -> Response(OK).body(r.path("name")!!) }
        )
        val sse = sse(
            "/hello" bind sse(
                "/{name}" bind { sse: Sse ->
                    println(sse.connectRequest)
                    val name = sse.connectRequest.path("name")!!
                    sse.send(Event("event", "hello $name", "123"))
                    sse.send(Data("goodbye $name"))
                    sse.close()
                },
                "/binary" bind { sse: Sse ->
                    println(sse.connectRequest)
                    val name = sse.connectRequest.path("name")!!
                    sse.send(Data("goodbye $name".byteInputStream()))
                    sse.close()
                },
                "/close" bind { sse: Sse ->
                    println(sse.connectRequest)
                    sse.onClose { println("closing") }
                }
            )
        )

        server = PolyHandler(http, sse = sse).asServer(serverConfig(0)).start()
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
    fun `can receive messages from sse`() {
        val handler = TestHandler()
        val client = EventSource.Builder(handler,
            URI.create("http://localhost:${server.port()}/hello/bob"))
            .build()

        client.start()

        Thread.sleep(5000)


//        println(JavaHttpClient()(Request(GET, "/hello/bob")).bodyString())
//        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/hello/bob"))
//
//        client.send(WsMessage("hello"))
//        assertThat(
//            client.received().take(2).toList(),
//            equalTo(listOf(WsMessage("bob"), WsMessage("goodbye bob".byteInputStream())))
//        )
    }
//
//    @Test
//    fun `should propagate close on client close`() {
//        val latch = CountDownLatch(1)
//        var closeStatus = false
//
//        val server = sse(
//            "/closes" bind { sse: Sse ->
//                sse.onClose {
//                    closeStatus = true
//                    latch.countDown()
//                }
//            }).asServer(serverConfig(0)).start()
//        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${server.port()}/closes"))
//        client.close()
//
//        latch.await()
//        assertThat(closeStatus, present())
//        server.close()
//    }
//
//    @Test
//    fun `should propagate close on server close`() {
//        val latch = CountDownLatch(1)
//        var closeStatus = false
//        val server = sse(
//            "/closes" bind { sse: Sse ->
//                closeStatus = true
//                sse.close()
//            }).asServer(serverConfig(0)).start()
//
//        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${server.port()}/closes"))
//        client.send(WsMessage("message"))
//
//        latch.await()
//        assertThat(closeStatus, present())
//        client.close()
//        server.close()
//    }
//
//    @Test
//    fun `should propagate close on server stop`() {
//        val latch = CountDownLatch(1)
//        var closeStatus = false
//
//        val server = sse(
//            "/closes" bind { sse: Sse ->
//                sse.onClose {
//                    closeStatus = true
//                    latch.countDown()
//                }
//            }).asServer(serverConfig(0)).start()
//        val client = WebsocketClient.blocking(Uri.of("ws://localhost:${server.port()}/closes"))
//        client.send(WsMessage("message"))
//        server.close()
//
//        latch.await()
//        assertThat(closeStatus, present())
//        client.close()
//    }
//
//    @Test
//    fun `should correctly set query parameters on upgrade request passed into the web socket`() {
//        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/queries?query=foo"))
//        client.send(WsMessage("hello"))
//        assertThat(client.received().take(1).toList(), equalTo(listOf(WsMessage("foo"))))
//    }
//
//    @Test
//    fun `can connect with non-blocking client`() {
//        val client = WebsocketClient.nonBlocking(Uri.of("ws://localhost:$port/hello/bob"))
//        val latch = CountDownLatch(1)
//        client.onMessage {
//            latch.countDown()
//        }
//
//        latch.await()
//    }
//
//    @Test
//    fun `should fail on invalid url`() {
//        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/aaa"))
//        assertThat({
//            client.send(WsMessage("hello"))
//        }, throws<WebsocketNotConnectedException>())
//    }
}


class TestHandler : EventHandler {
    val events = mutableListOf<String>()
    override fun onOpen() {
        println("open")
    }

    override fun onClosed() {
        println("closed")
    }

    override fun onMessage(event: String, messageEvent: MessageEvent) {
        println(event)
        println(messageEvent)
        events += event
    }

    override fun onComment(comment: String) {
        println(comment)
    }

    override fun onError(t: Throwable) {
        println(t.localizedMessage)
    }
}
