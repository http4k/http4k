package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.or
import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class SseServerContract(private val serverConfig: (Int) -> PolyServerConfig, private val client: HttpHandler) {
    private lateinit var server: Http4kServer

    private val http = routes(
        "/hello/{name}" bind { r: Request -> Response(OK).body(r.path("name")!!) }
    )

    private val sse = sse(
        "/hello" bind sse(
            "/{name}" bind { sse: Sse ->
                val name = sse.connectRequest.path("name")!!
                sse.send(Event("event1", "hello $name", "123"))
                sse.send(Event("event2", "again $name\nHi!", "456"))
                sse.send(Data("goodbye $name".byteInputStream()))
            }
        )
    )

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
                    Event("event2", "again bob\nHi!", "456"),
                    Data("goodbye bob".base64Encode())
                )
            )
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
                    Event("event2", "again leia\nHi!", "456"),
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
                    Event("event2", "again luke\nHi!", "456"),
                    Data("goodbye luke".base64Encode())
                )
            )
        )

        assertThat(
            client3.received().take(3).toList(),
            equalTo(
                listOf(
                    Event("event1", "hello anakin", "123"),
                    Event("event2", "again anakin\nHi!", "456"),
                    Data("goodbye anakin".base64Encode())
                )
            )
        )

        client2.close()
        client3.close()
    }

    @Test
    fun `when no http handler messages without the event stream header don't blow up`() {
        PolyHandler(sse = sse).asServer(serverConfig(0)).start().use {
            assertThat(
                client(Request(GET, "http://localhost:${it.port()}/hello/bob")),
                hasStatus(BAD_REQUEST) or hasStatus(NOT_FOUND)
            )
        }
    }
}
