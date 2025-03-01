package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bindHttp
import org.http4k.routing.bindSse
import org.http4k.routing.bindWs
import org.http4k.routing.poly
import org.http4k.routing.sse
import org.http4k.routing.websockets
import org.http4k.sse.SseMessage
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test

class PolyHandlerTestClientTest {

    @Test
    fun http() {
        val poly = PolyHandlerTestClient(poly("" bindHttp { _: Request -> Response(OK) }))

        assertThat(poly.http(Request(GET, "")), hasStatus(OK))
    }

    @Test
    fun ws() {
        val poly = PolyHandlerTestClient(poly("" bindWs websockets {
            it.send(WsMessage("hello"))
            it.send(WsMessage("world"))
            it.close()
        }))

        assertThat(
            poly.ws(Request(GET, "")).received().toList(),
            equalTo(listOf(WsMessage("hello"), WsMessage("world")))
        )
    }

    @Test
    fun sse() {
        val poly = PolyHandlerTestClient(poly("" bindSse sse {
            it.send(SseMessage.Data("hello"))
            it.send(SseMessage.Event("data", "hello"))
            it.close()
        }))

        assertThat(
            poly.sse(Request(GET, "")).received().toList(),
            equalTo(listOf(SseMessage.Data("hello"), SseMessage.Event("data", "hello")))
        )
    }
}
