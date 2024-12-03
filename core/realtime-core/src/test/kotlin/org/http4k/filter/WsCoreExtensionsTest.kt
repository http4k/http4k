package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import io.mockk.mockk
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.with
import org.http4k.lens.RequestContextKey
import org.http4k.testing.testWsClient
import org.http4k.util.TickingClock
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.http4k.websocket.WsStatus.Companion.BUGGYCLOSE
import org.http4k.websocket.then
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicReference

class WsCoreExtensionsTest {
    private val contexts = RequestContexts()
    private val key = RequestContextKey.required<Credentials>(contexts)
    private val credentials = Credentials("123", "456")

    @Test
    fun `can initialise and populate sse request context`() {
        val found = AtomicReference<Credentials>(null)
        val handler = ServerFilters.InitialiseWsRequestContext(contexts)
            .then(WsFilter { next ->
                {
                    next(it.with(key of credentials))
                }
            })
            .then {
                found.set(key(it))
                WsResponse { _ -> }
            }

        handler(Request(GET, "/"))

        assertThat(found.get(), equalTo(credentials))
    }

    @Test
    fun `can initialise and populate ws request context`() {
        val found = AtomicReference<Credentials>(null)
        val handler = ServerFilters.InitialiseWsRequestContext(contexts)
            .then(WsFilter { next ->
                {
                    next(it.with(key of credentials))
                }
            })
            .then {
                found.set(key(it))
                WsResponse { _ -> }
            }

        handler(Request(GET, "/"))

        assertThat(found.get(), equalTo(credentials))
    }

    @Test
    fun `can set subprotocol on WsResponse`() {
        val handler = ServerFilters.SetWsSubProtocol("foobar")
            .then { WsResponse { _ -> } }
        assertThat(handler(Request(GET, "/")).subprotocol, equalTo("foobar"))
    }

    @Test
    fun `debug request`() {
        val os = ByteArrayOutputStream()
        val req = Request(POST, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintWsResponse(PrintStream(os))
            .then {
                WsResponse { ws ->
                    ws.send(WsMessage("hello", Text))
                    ws.send(WsMessage("hello", Binary))
                }
            }

        socket.testWsClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** WS SEND POST:  -> Text: hello"))
        assertThat(actual, containsSubstring("***** WS SEND POST:  -> Binary: <<stream>>"))
    }

    @Test
    fun `debug response`() {
        val os = ByteArrayOutputStream()
        val req = Request(POST, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintWsResponse(PrintStream(os))
            .then {
                WsResponse { ws ->
                    ws.send(WsMessage("hello"))
                    ws.close()
                }
            }

        socket.testWsClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** WS RESPONSE null to POST:  *****"))
        assertThat(actual, containsSubstring("***** WS SEND POST:  -> Text: hello"))
        assertThat(actual, containsSubstring("***** WS CLOSED with 1000 on POST:  *****"))
    }

    @Test
    fun `catch all`() {
        val error = AtomicReference<Throwable>()
        val e = Exception("foo")

        val wsr = ServerFilters.CatchAllWs({
            error.set(it)
            WsResponse {
                it.close(BUGGYCLOSE)
            }
        }).then { throw e }(Request(GET, ""))
        wsr(object : Websocket by mockk() {
            override fun close(status: WsStatus) {
                assertThat(status, equalTo(BUGGYCLOSE))
            }
        })

        assertThat(error.get(), equalTo(e))
    }

    @Test
    fun `reporting latency for request`() {
        var called = false
        val request = Request(GET, "")
        val response = WsResponse { it.close() }

        val tickingClock = TickingClock()
        val socket = ResponseFilters.ReportWsTransaction(tickingClock) { (req, resp, duration) ->
            called = true
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(ofSeconds(1)))
        }.then { response }
        socket.testWsClient(request)
        assertTrue(called)
    }
}
