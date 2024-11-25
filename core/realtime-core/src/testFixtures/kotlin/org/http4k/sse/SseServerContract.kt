package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.or
import org.http4k.base64Encode
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.http4k.routing.bind as hbind

abstract class SseServerContract(
    private val serverConfig: (Int) -> PolyServerConfig,
    private val client: HttpHandler
) {

    private lateinit var server: Http4kServer

    private val http = routes(
        "/hello/{name}" hbind { r: Request -> Response(OK).body(r.path("name")!!) }
    )

    private val sse = sse(
        "/modify" bind {
            SseResponse(ACCEPTED) {
                it.close()
            }
        },
        "/routeMethod" bind sse(
            POST to {
                SseResponse(OK, listOf("METHOD" to it.method.name)) {
                    it.close()
                }
            },
            GET to {
                SseResponse(OK, listOf("METHOD" to it.method.name)) {
                    it.close()
                }
            }
        ),
        "/method" bind {
            SseResponse(OK, listOf("method" to it.method.name)) {
                it.close()
            }
        },
        "/hello" bind sse(
            "/{name}" bind { req: Request ->
                when {
                    req.query("reject") == null -> SseResponse(OK, listOf("foo" to "bar")) { sse ->
                        val name = req.path("name")!!
                        sse.send(Event("event1", "hello $name", "123"))
                        sse.send(Event("event2", "again $name", "456"))
                        sse.send(Data("goodbye $name".byteInputStream()))
                        sse.close()
                    }

                    else -> SseResponse {
                        it.close()
                    }
                }
            },
        ),
        "/newline" bind {
            SseResponse(OK) { sse ->
                sse.send(Event("event", "hello\nworld!", "456"))
                sse.close()
            }
        })

    @BeforeEach
    fun before() {
        server = PolyHandler(http, sse = sse).asServer(serverConfig(0)).start()
    }

    @AfterEach
    fun after() {
        server.stop()
    }

    @Test
    fun `can do standard http traffic`() {
        assertThat(client(Request(GET, "http://localhost:${server.port()}/hello/bob")), hasBody("bob"))
    }

    @Test
    fun `can receive messages from sse`() {
        val client = BlockingSseClient(Uri.of("http://localhost:${server.port()}/hello/bob"))

        assertThat(
            client.received().take(3).toList(),
            equalTo(
                listOf(
                    Event("event1", "hello bob", "123"),
                    Event("event2", "again bob", "456"),
                    Data("goodbye bob".base64Encode())
                )
            )
        )
    }

    @Test
    fun `supports methods`() {
        setOf(GET, PUT, DELETE, PATCH, POST).forEach {
            val response = JavaHttpClient()(
                Request(it, "http://localhost:${server.port()}/method")
                    .header("Accept", ContentType.TEXT_EVENT_STREAM.value)
            )
            assertThat(response.header("method"), equalTo(it.name))
        }
    }

    @Test
    fun `can route to method`() {
        setOf(GET, POST).forEach {
            val response = JavaHttpClient()(
                Request(it, "http://localhost:${server.port()}/routeMethod")
                    .header("Accept", ContentType.TEXT_EVENT_STREAM.value)
            )
            assertThat(response.header("METHOD"), equalTo(it.name))
        }
    }

    @Test
    fun `can handle multiple messages`() {
        val response = JavaHttpClient()(
            Request(GET, "http://localhost:${server.port()}/hello/leia")
                .header("Accept", ContentType.TEXT_EVENT_STREAM.value)
        )
        assertThat(response.header("foo"), equalTo("bar"))
        assertThat(response.bodyString(), containsSubstring("""id:123"""))
        assertThat(response.bodyString(), containsSubstring("""data:hello leia"""))
        assertThat(response.bodyString(), containsSubstring("""id:456"""))
        assertThat(response.bodyString(), containsSubstring("""event:event2"""))
        assertThat(response.bodyString(), containsSubstring("""data:again leia"""))
        assertThat(response.bodyString(), containsSubstring("""data:Z29vZGJ5ZSBsZWlh"""))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    open fun `can handle newlines`() {
        val response = JavaHttpClient()(
            Request(GET, "http://localhost:${server.port()}/newline")
                .header("Accept", ContentType.TEXT_EVENT_STREAM.value)
        )
        assertThat(response.bodyString(), containsSubstring("""id:456"""))
        assertThat(response.bodyString(), containsSubstring("""data:hello"""))
        assertThat(response.bodyString(), containsSubstring("""data:world"""))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    open fun `can modify status`() {
        val response = JavaHttpClient()(
            Request(GET, "http://localhost:${server.port()}/modify")
                .header("Accept", ContentType.TEXT_EVENT_STREAM.value)
        )
        assertThat(response.status, equalTo(ACCEPTED))
    }

    @Test
    fun `can route by method`() {
        setOf(GET, POST).forEach {
            val response = JavaHttpClient()(
                Request(it, "http://localhost:${server.port()}/routeMethod")
                    .header("Accept", ContentType.TEXT_EVENT_STREAM.value)
            )
            assertThat(response.status, equalTo(OK))
            assertThat(response.header("METHOD"), equalTo(it.name))
        }
    }

    @Test
    fun `can reject request`() {
        val client = BlockingSseClient(Uri.of("http://localhost:${server.port()}/hello/bob?reject=true"))

        assertThat(
            client.received().toList(),
            equalTo(listOf())
        )
    }

    @Test
    fun `can receive messages from sse using multiple clients`() {
        val client1 = BlockingSseClient(Uri.of("http://localhost:${server.port()}/hello/leia"))
        val client2 = BlockingSseClient(Uri.of("http://localhost:${server.port()}/hello/luke"))
        val client3 = BlockingSseClient(Uri.of("http://localhost:${server.port()}/hello/anakin"))

        assertThat(
            client1.received().take(3).toList(),
            equalTo(
                listOf(
                    Event("event1", "hello leia", "123"),
                    Event("event2", "again leia", "456"),
                    Data("goodbye leia".base64Encode())
                )
            )
        )
        client1.close()

        assertThat(
            client2.received().take(3).toList(),
            equalTo(
                listOf(
                    Event("event1", "hello luke", "123"),
                    Event("event2", "again luke", "456"),
                    Data("goodbye luke".base64Encode())
                )
            )
        )

        assertThat(
            client3.received().take(3).toList(),
            equalTo(
                listOf(
                    Event("event1", "hello anakin", "123"),
                    Event("event2", "again anakin", "456"),
                    Data("goodbye anakin".base64Encode())
                )
            )
        )

        client2.close()
        client3.close()
    }

    @Test
    open fun `when no http handler messages without the event stream header don't blow up`() {
        PolyHandler(sse = sse).asServer(serverConfig(0)).start().use {
            assertThat(
                client(Request(GET, "http://localhost:${it.port()}/hello/bob")),
                hasStatus(BAD_REQUEST) or hasStatus(NOT_FOUND)
            )
        }
    }
}
