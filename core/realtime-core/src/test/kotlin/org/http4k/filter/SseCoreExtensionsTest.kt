package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.with
import org.http4k.lens.RequestContextKey
import org.http4k.sse.SseFilter
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.http4k.testing.testSseClient
import org.http4k.util.TickingClock
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicReference

class SseCoreExtensionsTest {
    private val contexts = RequestContexts()
    private val key = RequestContextKey.required<Credentials>(contexts)
    private val credentials = Credentials("123", "456")

    @Test
    fun `can initialise and populate sse request context`() {
        val found = AtomicReference<Credentials>(null)
        val handler = ServerFilters.InitialiseSseRequestContext(contexts)
            .then(SseFilter { next ->
                {
                    next(it.with(key of credentials))
                }
            })
            .then {
                found.set(key(it))
                SseResponse { _ -> }
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
        val req = Request(GET, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintSseRequest(PrintStream(os))
            .then {
                SseResponse { sse ->
                    sse.send(SseMessage.Data("hello"))
                }
            }

        socket.testSseClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** SSE REQUEST: GET:  *****"))
        assertThat(actual, containsSubstring("<<stream>>"))
    }

    @Test
    fun `debug response`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintSseResponse(PrintStream(os))
            .then {
                SseResponse { sse ->
                    sse.send(SseMessage.Data("hello"))
                    sse.close()
                }
            }

        socket.testSseClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** SSE RESPONSE 200 to GET:  *****"))
        assertThat(actual, containsSubstring("***** SSE SEND GET:  -> Data"))
        assertThat(actual, containsSubstring("data: hello"))
        assertThat(actual, containsSubstring("***** SSE CLOSED on GET:  *****"))
    }

    @Test
    fun `catch all`() {
        val error = AtomicReference<Throwable>()
        val e = Exception("foo")

        val response = ServerFilters.CatchAllSse({
            error.set(it)
            SseResponse(I_M_A_TEAPOT) {
                it.close()
            }
        }).then { throw e }(Request(GET, ""))

        assertThat(response.status, equalTo(I_M_A_TEAPOT))
        assertThat(error.get(), equalTo(e))
    }

    @Test
    fun `reporting latency for request`() {
        var called = false
        val request = Request(GET, "")
        val response = SseResponse { it.close() }

        val tickingClock = TickingClock()
        val socket = ResponseFilters.ReportSseTransaction(tickingClock) { (req, resp, duration) ->
            called = true
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(ofSeconds(1)))
        }.then { response }
        socket.testSseClient(request)
        assertTrue(called)
    }
}
