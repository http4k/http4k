package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.DatastarEvent.MergeSignals
import org.http4k.datastar.Signal
import org.http4k.filter.debug
import org.http4k.lens.accept
import org.http4k.lens.datastarFragments
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.server.Http4kServer
import org.http4k.server.PolyHandler
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Event
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.http4k.routing.bind as hbind

abstract class DatastarServerContract(
    private val serverConfig: (Int) -> PolyServerConfig,
    private val client: HttpHandler
): PortBasedTest {

    private lateinit var server: Http4kServer

    private val sse = sse(
        "/signal" bind sse(GET to sse {
            it.send(MergeSignals(Signal.of("oh signal1")).toSseEvent())
            it.send(MergeSignals(Signal.of("oh signal2")).toSseEvent())
            it.close()
        })
    ).debug()

    @BeforeEach
    fun before() {
        server = PolyHandler(
            http = routes("/noStream" hbind {
                Response(OK).datastarFragments(DatastarEvent.MergeFragments("hello"))
            }),
            sse = sse
        ).asServer(serverConfig(0)).start()
    }

    @AfterEach
    fun after() {
        server.stop()
    }

    @Test
    fun `can receive messages from sse`() {
        val client = BlockingSseClient(Uri.of("http://localhost:${server.port()}/signal"))

        val toList = client.received().toList()
        assertThat(
            toList,
            equalTo(
                listOf(
                    Event("datastar-merge-signals", "signals oh signal1\nonlyIfMissing false"),
                    Event("datastar-merge-signals", "signals oh signal2\nonlyIfMissing false")
                )
            )
        )
    }

    @Test
    fun `can receive messages via http`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/noStream"))

        assertThat(
            response.bodyString(),
            equalTo(
                """event: datastar-merge-fragments
data: fragments hello
data: settleDuration 300
data: mergeMode morph
data: useViewTransition false

"""
            )
        )
    }

    @Test
    fun `can receive messages via client`() {
        val response = client(
            Request(
                GET,
                "http://localhost:${server.port()}/signal"
            ).accept(ContentType.TEXT_EVENT_STREAM)
        )

        val actual = response.bodyString()
        val expected = """event:datastar-merge-signals
data:signals oh signal1
data:onlyIfMissing false

event:datastar-merge-signals
data:signals oh signal2
data:onlyIfMissing false

"""
        assertThat(
            actual,
            equalTo(
                expected
            )
        )
    }
}
