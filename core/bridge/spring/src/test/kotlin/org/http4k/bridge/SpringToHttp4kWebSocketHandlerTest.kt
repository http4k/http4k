package org.http4k.bridge

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import dev.forkhandles.mock4k.MockMode.Relaxed
import dev.forkhandles.mock4k.mock
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.net.URI
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class SpringToHttp4kWebSocketHandlerTest {

    private val session = object : WebSocketSession by mock<WebSocketSession>(Relaxed) {
        val sent = mutableListOf<WebSocketMessage<*>>()
        var closed: CloseStatus? = null
        private val id = UUID.randomUUID().toString()
        override fun getId() = id
        override fun getUri() = URI("/ws/things?x=1")
        override fun getHandshakeHeaders() = HttpHeaders().apply {
            add("Sec-WebSocket-Version", "13")
            add("Origin", "https://example.com")
        }
        override fun sendMessage(message: WebSocketMessage<*>) {
            sent += message
        }
        override fun close(status: CloseStatus) { closed = status }
    }

    @Test
    fun `text messages are routed in and out of the http4k handler`() {
        val received = mutableListOf<WsMessage>()
        val handler = SpringToHttp4kWebSocketHandler { rq ->
            assertThat(rq.uri.path, equalTo("/ws/things"))
            assertThat(rq.uri.query, equalTo("x=1"))
            assertThat(rq.header("Sec-WebSocket-Version"), equalTo("13"))
            WsResponse { ws ->
                ws.onMessage { received += it; ws.send(WsMessage("echo:" + it.bodyString())) }
            }
        }

        handler.afterConnectionEstablished(session)
        handler.handleMessage(session, TextMessage("hi"))
        handler.handleMessage(session, TextMessage("there"))

        assertThat(received.map { it.bodyString() }, equalTo(listOf("hi", "there")))
        assertThat(session.sent, hasSize(equalTo(2)))
        assertThat((session.sent[0] as TextMessage).payload, equalTo("echo:hi"))
        assertThat((session.sent[1] as TextMessage).payload, equalTo("echo:there"))
    }

    @Test
    fun `binary messages roundtrip`() {
        val handler = SpringToHttp4kWebSocketHandler {
            WsResponse { ws -> ws.onMessage { ws.send(it) } }
        }
        handler.afterConnectionEstablished(session)

        handler.handleMessage(session, BinaryMessage(ByteBuffer.wrap(byteArrayOf(1, 2, 3))))

        assertThat(session.sent, hasSize(equalTo(1)))
        val out = session.sent.single() as BinaryMessage
        val bytes = ByteArray(out.payload.remaining()).also { out.payload.duplicate().get(it) }
        assertThat(bytes.toList(), equalTo(listOf<Byte>(1, 2, 3)))
    }

    @Test
    fun `triggers close handler on disconnect`() {
        val closeStatus = AtomicReference<WsStatus>()
        val handler = SpringToHttp4kWebSocketHandler {
            WsResponse { ws -> ws.onClose { closeStatus.set(it) } }
        }
        handler.afterConnectionEstablished(session)

        handler.afterConnectionClosed(session, CloseStatus.GOING_AWAY)

        assertThat(closeStatus.get().code, equalTo(1001))
    }

    @Test
    fun `triggers error handler on transport error`() {
        val errors = mutableListOf<Throwable>()
        val handler = SpringToHttp4kWebSocketHandler {
            WsResponse { ws -> ws.onError { errors += it } }
        }
        handler.afterConnectionEstablished(session)

        handler.handleTransportError(session, RuntimeException("boom"))

        assertThat(errors, hasSize(equalTo(1)))
        assertThat(errors.single().message!!, containsSubstring("boom"))
    }

    @Test
    fun `outbound close from http4k closes the spring session`() {
        var captured: org.http4k.websocket.Websocket? = null
        val handler = SpringToHttp4kWebSocketHandler {
            WsResponse { ws -> captured = ws }
        }
        handler.afterConnectionEstablished(session)

        captured!!.close(WsStatus(4000, "bye"))

        assertThat(session.closed?.code, equalTo(4000))
        assertThat(session.closed?.reason, equalTo("bye"))
    }
}
